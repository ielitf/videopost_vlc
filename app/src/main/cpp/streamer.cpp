#include<android/log.h>
#include <jni.h>
#include <string>
#include <unistd.h>
#include "vlc/vlc.h"
#include "include/vlc/libvlc.h"

libvlc_instance_t *vlc;
const char *media_name = "ceiv live android";
bool is_playing = true;

extern "C" JNIEXPORT jint JNICALL
Java_com_ceiv_streamer_Streamer_creatStream(
        JNIEnv *env,
        jobject thiz,
        jstring mediaPath,
        jstring soutArg,
        jint time) {

    const char *url = (env)->GetStringUTFChars(mediaPath, nullptr);
    const char *sout = (env)->GetStringUTFChars(soutArg, nullptr);

    printf("current sout: %s\n", sout);
    printf("media name: %s\n", url);

    vlc = libvlc_new(0, nullptr);
    libvlc_vlm_add_broadcast(vlc, media_name, url, sout, 0, nullptr, true, false);
    libvlc_vlm_play_media(vlc, media_name);

//    is_playing = false;
//    while(is_playing) {
//        usleep(10000000);
//    };
    sleep(time);
    libvlc_vlm_stop_media(vlc, media_name);
    libvlc_vlm_release(vlc);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ceiv_streamer_Streamer_stopStream(
        JNIEnv *env,
        jobject thiz) {

    if(vlc == nullptr) return -1;
    is_playing = false;
    libvlc_vlm_stop_media(vlc, media_name);
    libvlc_vlm_release(vlc);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ceiv_streamer_Streamer_startStream(
        JNIEnv *env,
        jobject thiz) {

    if(vlc == nullptr) return -1;
    is_playing = true;
    libvlc_vlm_play_media(vlc, media_name);
    while(is_playing);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ceiv_streamer_Streamer_pauseStream(
        JNIEnv *env,
        jobject thiz) {

    if(vlc == nullptr) return -1;
    libvlc_vlm_pause_media(vlc, media_name);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ceiv_streamer_Streamer_addInput(
        JNIEnv *env,
        jobject thiz,
        jstring mediaPath) {

    if(vlc == nullptr) return -1;
    const char *media_name = (env)->GetStringUTFChars(mediaPath, nullptr);
    libvlc_vlm_add_input(vlc, media_name, nullptr);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ceiv_streamer_Streamer_delMedia(
        JNIEnv *env,
        jobject thiz,
        jstring mediaPath) {

    if(vlc == nullptr) return -1;
    const char *media_name = (env)->GetStringUTFChars(mediaPath, nullptr);
    libvlc_vlm_del_media(vlc, media_name);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ceiv_streamer_Streamer_changeMedia(
        JNIEnv *env,
        jobject thiz,
        jstring mediaPath,
        jstring soutArg) {

    if(vlc == nullptr) return -1;
    const char *url = (env)->GetStringUTFChars(mediaPath, nullptr);
    const char *sout = (env)->GetStringUTFChars(soutArg, nullptr);
    libvlc_vlm_change_media(vlc, media_name, url, sout, 0, nullptr, true, true);
    return 0;
}