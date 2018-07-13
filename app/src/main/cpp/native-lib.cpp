#include <jni.h>
#include <string>


#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <android/log.h>

using namespace std;
using namespace cv;

extern "C"
JNIEXPORT void JNICALL

Java_com_example_sarthak_retina_MainActivity_imageFromJNI(JNIEnv *env, jobject instance,jlong inpAddr,jlong outAddr) {
    vector<Mat> rgb(3);
    Mat processed_image;
    Mat sample;
    Mat labels,centers;
    double threshold_value_x,threshold_value_y,threshold_value;

    Mat &src = *(Mat *) inpAddr;
    Mat &dst = *(Mat *) outAddr;

    split(src, rgb);
    Mat element = getStructuringElement(MORPH_ELLIPSE, Size(21, 21));
    morphologyEx(rgb[0], processed_image, MORPH_CLOSE, element);

    //processed_image.convertTo(sample,CV_32F);
    //__android_log_print(ANDROID_LOG_ERROR, "Image Dimensions: ", "%d", processed_image.dims);

    //kmeans(sample,6,labels,TermCriteria(TermCriteria::EPS + TermCriteria::COUNT,100,0.01),3,KMEANS_RANDOM_CENTERS,centers);
    //cerr << centers;
    processed_image.copyTo(dst);
}