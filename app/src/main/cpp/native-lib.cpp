#include <jni.h>
#include <string>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>


using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_sarthak_retina_MainActivity_imageFromJNI(JNIEnv *env, jobject instance,
                                                          jlong ipImage, jlong opImage) {
    Mat &mat = *(Mat *) ipImage;
    Mat &op_mat = *(Mat *) opImage;

    Mat bgr[3];
    split(ipImage,bgr[2]);

    op_mat = bgr[2].clone();

}