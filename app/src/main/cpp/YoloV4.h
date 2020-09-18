#ifndef YOLOV4_H
#define YOLOV4_H

#include "ncnn/net.h"

namespace yolocv{
    typedef struct{
        int width;
        int height;
    }YoloSize;
}

typedef struct BoxInfo {
    float x1;
    float y1;
    float x2;
    float y2;
    float score;
    int label;
} BoxInfo;

class YoloV4 {
public:
    YoloV4(AAssetManager* mgr, const char* param, const char* bin);
    ~YoloV4();
    std::vector<BoxInfo> detect(JNIEnv* env, jobject image, float threshold, float nms_threshold);
    std::vector<std::string> labels{"with mask incorrectly", "with mask", "no mask"};
private:
    static std::vector<BoxInfo> decode_infer(ncnn::Mat &data, const yolocv::YoloSize& frame_size, int net_size,int num_classes,float threshold);
//    static void nms(std::vector<BoxInfo>& result,float nms_threshold);
    ncnn::Net* Net;
    int input_size = 640/2;
    int num_class = 3;
public:
    static YoloV4 *detector;
    static bool hasGPU;
};


#endif //YOLOV4_H
