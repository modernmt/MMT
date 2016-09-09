//
// Created by Davide  Caroselli on 09/12/15.
//

#ifndef JNIMOSES_JTRANSLATION_H
#define JNIMOSES_JTRANSLATION_H

#include <jni.h>
#include <string>
#include <vector>

class JTranslation {
    jmethodID constructor;

public:
    const jclass _class;

    JTranslation(JNIEnv *);

    jintArray getAlignment(JNIEnv *jvm, std::vector <std::pair<size_t, size_t>> alignment);

    jobject create(JNIEnv *jvm, std::string &text, jobjectArray nbestList, jintArray alignment);
};

class JHypothesis {
    jmethodID constructor;

public:
    const jclass _class;

    JHypothesis(JNIEnv *);

    jobject create(JNIEnv *jvm, std::string &text, float totalScore, std::string &fvals);
};


#endif //JNIMOSES_JTRANSLATION_H
