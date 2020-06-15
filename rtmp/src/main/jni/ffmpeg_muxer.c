/*
 * Copyright (c) 2019 Illuminoo Sports BV.
 * This file is part of LISA and subject to the to the terms and conditions defined in file 'LICENSE',
 * which is part of this source code package.
 */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <libavutil/avassert.h>
#include <libavutil/channel_layout.h>
#include <libavutil/opt.h>
#include <libavutil/mathematics.h>
#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <android/log.h>

#define TAG "FFMPEG"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,    TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,     TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,     TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,    TAG, __VA_ARGS__)
#define ARRAY_ELEMS(a)  (sizeof(a) / sizeof(a[0]))

/**
 * A wrapper for a single output stream
 */
typedef struct OutputStream {

    // Format
    AVOutputFormat *format;

    // Context
    AVFormatContext *context;

    // Additional options
    AVDictionary *options;

    // Video stream
    AVStream *video;

    // Audio stream
    AVStream *audio;

    // Last packet sent
    AVPacket *pkt;

} OutputStream;

/**
 * Log packet
 * @param fmt_ctx Format context
 * @param pkt Packet
 */
static void log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt) {
    AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;

    LOGD("pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d key_frame:%d payload:%d",
         av_ts2str(pkt->pts), av_ts2timestr(pkt->pts, time_base),
         av_ts2str(pkt->dts), av_ts2timestr(pkt->dts, time_base),
         av_ts2str(pkt->duration), av_ts2timestr(pkt->duration, time_base),
         pkt->stream_index, pkt->flags, pkt->size);
}

/**
 * Add an output video stream
 * @param env JVM
 * @param cls Java class
 * @param id Pointer to output stream
 * @param width Input width
 * @param height Input height
 * @param fps Frames per second
 * @param gop Group of Pictures
 * @param bitrate Bitrate (bps)
 * @return Stream index (or -1 in case of error)
 */
static jint
add_video_stream(JNIEnv *env, jclass cls, jlong id, jint type, jint width, jint height,
                 jint fps, jint gop, jint bitrate) {
    jint ret = 0;
    OutputStream *output = (struct OutputStream *) id;

    // Add video stream to output
    output->video = avformat_new_stream(output->context, NULL);
    if (output->video == NULL) {
        LOGE("Could not add video stream");
        return -1;
    }
    output->video->index = output->context->nb_streams - 1;
    output->video->id = output->video->index;
    output->video->time_base = (AVRational) {1, 1000};
    output->video->avg_frame_rate = (AVRational) {fps, 1};

    // Set codec parameters
    AVCodecParameters *params = output->video->codecpar;
    params->codec_type = AVMEDIA_TYPE_VIDEO;
    params->codec_id = AV_CODEC_ID_H264;
    if (type == 1) params->codec_id = AV_CODEC_ID_HEVC;
    params->bit_rate = bitrate;
    params->width = width;
    params->height = height;
    params->format = AV_PIX_FMT_YUV420P;

    return output->video->index;
}

/**
 * Add an output audio stream
 * @param env JVM
 * @param cls Java class
 * @param id Pointer to output stream
 * @param type Codec type
 * @param sample_rate Sample rate
 * @param bitrate Bitrate (bps)
 * @return Stream index (or -1 in case of error)
 */
static jint
add_audio_stream(JNIEnv *env, jclass cls, jlong id, jint type, jint sample_rate, jint bitrate) {
    jint ret = 0;
    OutputStream *output = (struct OutputStream *) id;

    // Add audio stream to output
    output->audio = avformat_new_stream(output->context, NULL);
    if (output->audio == NULL) {
        LOGE("Could not add audio stream");
        return -1;
    }
    output->audio->index = output->context->nb_streams - 1;
    output->audio->id = output->audio->index;
    output->audio->time_base = (AVRational) {1, 1000};

    // Set codec params
    AVCodecParameters *params = output->audio->codecpar;
    params->codec_type = AVMEDIA_TYPE_AUDIO;
    params->codec_id = AV_CODEC_ID_AAC;
    params->format = AV_SAMPLE_FMT_S16;
    params->bit_rate = bitrate;
    params->sample_rate = sample_rate;
    params->channel_layout = AV_CH_LAYOUT_STEREO;
    params->channels = av_get_channel_layout_nb_channels(params->channel_layout);

    return output->audio->index;
}

/**
 * Stream from incoming buffer
 * @param output Output
 * @param stream Stream
 * @param codec Input codec
 * @param parser Input parser
 * @param buffer Buffer
 * @param size Buffer size (bytes)
 * @param pts PTS (microseconds)
 * @param flags Flags
 * @return 0 when successful
 */
static jint
write_frame(OutputStream *output, AVStream *stream, jbyte *buffer, jint size, jlong pts, jint flags) {
    jint ret = 0;
    AVPacket *pkt = output->pkt;
    AVFormatContext *context = output->context;

    // Write header if not yet sent
    if (pkt == NULL) {
        if (flags == 2) {
            // Update stream info
            stream->codecpar->extradata_size = size;
            stream->codecpar->extradata = av_malloc(size);
            memcpy(stream->codecpar->extradata, buffer, size);
        }

        // TODO: Add video package conditional when video is ready
        // output->video->codecpar->extradata_size > 0
        if (output->audio->codecpar->extradata_size > 0) {

            // Write header
            ret = avformat_write_header(context, &output->options);
            if (ret < 0) {
                LOGE("Error occurred when writing header: %s", av_err2str(ret));
                return ret;
            }

            pkt = av_packet_alloc();
            output->pkt = pkt;
        }

        return 0;
    }

    // Prepare packet
    pkt->size = size;
    pkt->data = (uint8_t *) buffer;

    // PTS
    pkt->pts = av_rescale_q_rnd(pts, AV_TIME_BASE_Q, stream->time_base,
                                AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
    pkt->dts = pkt->pts;
    pkt->duration = 0;
    pkt->pos = -1;

    // Set index and flags
    pkt->stream_index = stream->index;
    if (flags == 1) pkt->flags = AV_PKT_FLAG_KEY;
    else pkt->flags = 0;

    // Send packet
//    log_packet(context, pkt);
    ret = av_write_frame(context, pkt);
    if (ret < 0) {
        LOGE("Error while writing packet: %s", av_err2str(ret));
        return ret;
    }

    return ret;
}

/**
 * Write encoded video packet to the muxer
 * @param env JVM
 * @param cls Java class
 * @param id Pointer to output stream
 * @param data Payload
 * @param len Payload size in bytes
 * @param pts PTS in microseconds
 * @param flags Flags (e.g. keyframe)
 * @return 1 when encoding is finished, 0 otherwise
 */
static jstring
write_video(JNIEnv *env, jclass cls, jlong id, jbyteArray *data, jint len, jlong pts, jint flags) {
    jbyte *_data = (*env)->GetByteArrayElements(env, data, 0);
    OutputStream *output = (struct OutputStream *) id;
    jint ret = write_frame(output, output->video, _data, len, pts, flags);
    (*env)->ReleaseByteArrayElements(env, data, _data, JNI_ABORT);

    if (ret < 0) return (*env)->NewStringUTF(env, av_err2str(ret));
    return NULL;
}

/**
 * Write encoded audio packet to the muxer
 * @param env JVM
 * @param cls Java class
 * @param id Pointer to output stream
 * @param data Payload
 * @param len Payload size in bytes
 * @param pts PTS in microseconds
 * @param flags Flags (optional)
 * @return error message if any (otherwise null)
 */
static jstring
write_audio(JNIEnv *env, jclass cls, jlong id, jbyteArray *data, jint len, jlong pts, jint flags) {
    jbyte *_data = (*env)->GetByteArrayElements(env, data, 0);
    OutputStream *output = (struct OutputStream *) id;
    jint ret = write_frame(output, output->audio, _data, len, pts, flags);
    (*env)->ReleaseByteArrayElements(env, data, _data, JNI_ABORT);

    if (ret < 0) return (*env)->NewStringUTF(env, av_err2str(ret));
    return NULL;
}

/**
 * Open connection
 * @param env JVM
 * @param cls Java class
 * @param url Destination URL
 * @param format Output format
 * @return 0 if successful
 */
static jlong open(JNIEnv *env, jclass cls, jstring *url, jstring *format) {
    const char *_url = (*env)->GetStringUTFChars(env, url, 0);
    const char *_format = (*env)->GetStringUTFChars(env, format, 0);
    OutputStream *output = malloc(sizeof(OutputStream));
    output->pkt = NULL;
    output->options = NULL;

    // Allocate the output media context
    jint ret = avformat_alloc_output_context2(&output->context, NULL, _format, _url);
    if (ret < 0) {
        output->format = NULL;
        LOGE("Error allocating streaming context for '%s' with output format '%s': %s", _url,
             _format, av_err2str(ret));
    } else {
        output->format = output->context->oformat;

        if (strcmp(_format, "dash") == 0) {
            ret -= av_dict_set(&output->options, "method", "PUT", 0);
            ret -= av_dict_set(&output->options, "streaming", "1", 0);
            ret -= av_dict_set(&output->options, "seg_duration", "5", 0);
            ret -= av_dict_set(&output->options, "http_persistent", "1", 0);
            ret -= av_dict_set(&output->options, "utc_timing_url", "https://time.akamai.com/?iso", 0);
            ret -= av_dict_set(&output->options, "index_correction", "1", 0);
            ret -= av_dict_set(&output->options, "use_timeline", "0", 0);
            ret -= av_dict_set(&output->options, "media_seg_name", "chunk-stream$RepresentationID$-$Number%05d$.m4s", 0);
            ret -= av_dict_set(&output->options, "init_seg_name", "init-stream$RepresentationID$.m4s", 0);
            ret -= av_dict_set(&output->options, "window_size", "5", 0);
            ret -= av_dict_set(&output->options, "extra_window_size", "10", 0);
            ret -= av_dict_set(&output->options, "remove_at_exit", "1", 0);
            ret -= av_dict_set(&output->options, "adaptation_sets", "id=0,streams=v id=1,streams=a", 0);

            if (ret < 0) {
                LOGE("Unable to set streaming options");
            }
        }

        // Open the output file, if needed
        if (!(output->format->flags & AVFMT_NOFILE)) {
            ret = avio_open(&output->context->pb, _url, AVIO_FLAG_WRITE);
            if (ret < 0) {
                LOGE("Could not open '%s': %s", _url, av_err2str(ret));
            }
        }
    }

    (*env)->ReleaseStringUTFChars(env, url, _url);
    (*env)->ReleaseStringUTFChars(env, format, _format);

    if (ret < 0) return ret;
    return (jlong) output;
}

/**
 * Close connection
 * @param env JVM
 * @param cls Java class
 * @param id Pointer to output stream
 */
static void close(JNIEnv *env, jclass cls, jlong id) {
    OutputStream *output = (struct OutputStream *) id;

    // Write trailer and free packet
    if (output->pkt != NULL) {
        av_write_trailer(output->context);
        av_packet_free(&output->pkt);
    }

    // Free the contexts
    avformat_free_context(output->context);

    /// Free output stream
    free(output);
}

/**
 * Exported JNI methods
 */
static JNINativeMethod export[] = {
        {"open",             "(Ljava/lang/String;Ljava/lang/String;)J", open},
        {"addVideoTrack",    "(JIIIIII)I",                              add_video_stream},
        {"addAudioTrack",    "(JIII)I",                                 add_audio_stream},
        {"writeVideoSample", "(J[BIJI)Ljava/lang/String;",              write_video},
        {"writeAudioSample", "(J[BIJI)Ljava/lang/String;",              write_audio},
        {"close",            "(J)V",                                    close},
};

/**
 * Register JNI methods on load
 * @param vm Java VM
 * @param reserved Reserved
 * @return JNI version if successful
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jint res = (*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6);
    if (res != JNI_OK || env == NULL) {
        LOGE("JNI environment not got");
        return JNI_ERR;
    }

    jclass clz = (*env)->FindClass(env, "dev/oscarreyes/rtmp/io/FFMpegMuxer");
    if (clz == NULL) {
        LOGE("Class FFMpegMuxer not found");
        return JNI_ERR;
    }

    res = (*env)->RegisterNatives(env, clz, export, ARRAY_ELEMS(export));
    if (res != JNI_OK) {
        LOGE("JNI methods not registered");
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
