#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <math.h>

// ncnn
#include "include/opencv.h"
#include <sys/time.h>
#include <unistd.h>
#include "include/net.h"

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator g_workspace_pool_allocator;

static ncnn::Mat ncnn_bin;
static ncnn::Net ncnn_net;

extern "C" {


// public native boolean Init(byte[] words,byte[] param, byte[] bin);　　原函数形式（c++） 以下形式为ndk的c++形式
JNIEXPORT jboolean JNICALL
Java_com_example_testncnn_NcnnWrapper_Init(JNIEnv *env, jobject obj, jstring param, jbyteArray bin) {
    __android_log_print(ANDROID_LOG_DEBUG, "NcnnWrapper", "enter the jni func");
    // init param
    {
        int len = env->GetStringLength(param);
        //ncnn_param.create(len, (size_t) 1u);
        //env->GetByteArrayRegion(param, 0, len, (jbyte *) ncnn_param);
        const char *nativeString = env->GetStringUTFChars(param, 0);
        int ret = ncnn_net.load_param_mem(nativeString);
        env->ReleaseStringUTFChars(param, nativeString);
        __android_log_print(ANDROID_LOG_DEBUG, "NcnnWrapper", "load_param %d %d", ret, len);
    }

    // init bin
    {
        int len = env->GetArrayLength(bin);
        ncnn_bin.create(len, (size_t) 1u);
        env->GetByteArrayRegion(bin, 0, len, (jbyte *) ncnn_bin);
        int ret = ncnn_net.load_model((const unsigned char *) ncnn_bin);
        __android_log_print(ANDROID_LOG_DEBUG, "NcnnWrapper", "load_model %d %d", ret, len);
    }

    ncnn::Option opt;
    opt.lightmode = true;
    opt.num_threads = 4;   //线程 这里可以修改
    opt.blob_allocator = &g_blob_pool_allocator;
    opt.workspace_allocator = &g_workspace_pool_allocator;

    ncnn_net.opt = opt;
    //ncnn::set_default_option(opt);

    return JNI_TRUE;
}

// public native String Detect(Bitmap bitmap);
JNIEXPORT jfloatArray JNICALL
Java_com_example_testncnn_NcnnWrapper_Detect(JNIEnv* env, jobject thiz, jobject bitmap)
{
    // ncnn from bitmap
    ncnn::Mat in;
    {
        AndroidBitmapInfo info;
        AndroidBitmap_getInfo(env, bitmap, &info);
//        int origin_w = info.width;
//        int origin_h = info.height;
//        int width = 300;
//        int height = 300;
        int width = info.width;
        int height = info.height;
        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
            return NULL;

        void* indata;
        AndroidBitmap_lockPixels(env, bitmap, &indata);
        // 把像素转换成data，并指定通道顺序
        // 因为图像预处理每个网络层输入的数据格式不一样一般为300*300 128*128等等所以这类需要一个resize的操作可以在cpp中写，也可以是java读入图片时有个resize操作
//      in = ncnn::Mat::from_pixels_resize((const unsigned char*)indata, ncnn::Mat::PIXEL_RGBA2RGB, origin_w, origin_h, width, height);

        in = ncnn::Mat::from_pixels((const unsigned char*)indata, ncnn::Mat::PIXEL_RGBA2RGB, width, height);

        // 下面一行为debug代码
        //__android_log_print(ANDROID_LOG_DEBUG, "MobilenetssdJniIn", "Mobilenetssd_predict_has_input1, in.w: %d; in.h: %d", in.w, in.h);
        AndroidBitmap_unlockPixels(env, bitmap);
    }

    // ncnn_net
    {
        // 归一化
        const float mean_vals[3] = {104.f, 117.f, 123.f};
        in.substract_mean_normalize(mean_vals, 0);

        ncnn::Extractor ex = ncnn_net.create_extractor();//前向传播

        // 如果不加密是使用ex.input("data", in);
        // BLOB_data在id.h文件中可见，相当于datainput网络层的id
        int result = ex.input("data", in);
        //ex.set_num_threads(4); 和上面一样一个对象

        ncnn::Mat out;
        // 如果时不加密是使用ex.extract("prob", out);
        //BLOB_detection_out.h文件中可见，相当于dataout网络层的id,输出检测的结果数据
        result = ex.extract("prob", out);

        int output_wsize = out.w;
        int output_hsize = out.h;

        //// Sigmoid, seg_out is an output ncnn mat and 输出整理
        jfloat *output[output_wsize * output_hsize];
        for(int i = 0; i< out.h; i++) {
            float* values = out.row(i);
            for (int j = 0; j < out.w; j++) {
                values[j] = 1.f / (1.f + exp(-values[j]));
                output[i*output_wsize + j] = &values[j];
            }
        }

        jfloatArray jOutputData = env->NewFloatArray(output_wsize * output_hsize);
        if (jOutputData == nullptr) return nullptr;
        env->SetFloatArrayRegion(jOutputData, 0,  output_wsize * output_hsize,
                                 reinterpret_cast<const jfloat *>(*output));

        return jOutputData;
    }
}
}
