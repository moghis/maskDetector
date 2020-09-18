// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#ifndef NCNN_PLATFORM_H
#define NCNN_PLATFORM_H

#define NCNN_STDIO 1
#define NCNN_STRING 1
#define NCNN_OPENCV 0
#define NCNN_SIMPLESTL 0
#define NCNN_THREADS 1
#define NCNN_BENCHMARK 0
#define NCNN_PIXEL 1
#define NCNN_PIXEL_ROTATE 1
#define NCNN_VULKAN 0
#define NCNN_VULKAN_ONLINE_SPIRV 1
#define NCNN_REQUANT 0
#define NCNN_RUNTIME_CPU 1
#define NCNN_AVX2 0
#define NCNN_ARM82 1

#if NCNN_THREADS
#if (defined _WIN32 && !(defined __MINGW32__))
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <process.h>
#else
#include <pthread.h>
#endif
#endif // NCNN_THREADS

#if __ANDROID_API__ >= 26
#define VK_USE_PLATFORM_ANDROID_KHR
#endif // __ANDROID_API__ >= 26

namespace ncnn {

#if NCNN_THREADS
#if (defined _WIN32 && !(defined __MINGW32__))
class Mutex
{
public:
    Mutex() { InitializeSRWLock(&srwlock); }
    ~Mutex() {}
    void lock() { AcquireSRWLockExclusive(&srwlock); }
    void unlock() { ReleaseSRWLockExclusive(&srwlock); }
private:
    friend class ConditionVariable;
    // NOTE SRWLock is available from windows vista
    SRWLOCK srwlock;
};
#else // _WIN32
class Mutex
{
public:
    Mutex() { pthread_mutex_init(&mutex, 0); }
    ~Mutex() { pthread_mutex_destroy(&mutex); }
    void lock() { pthread_mutex_lock(&mutex); }
    void unlock() { pthread_mutex_unlock(&mutex); }
private:
    friend class ConditionVariable;
    pthread_mutex_t mutex;
};
#endif // _WIN32

class MutexLockGuard
{
public:
    MutexLockGuard(Mutex& _mutex) : mutex(_mutex) { mutex.lock(); }
    ~MutexLockGuard() { mutex.unlock(); }
private:
    Mutex& mutex;
};

#if (defined _WIN32 && !(defined __MINGW32__))
class ConditionVariable
{
public:
    ConditionVariable() { InitializeConditionVariable(&condvar); }
    ~ConditionVariable() {}
    void wait(Mutex& mutex) { SleepConditionVariableSRW(&condvar, &mutex.srwlock, INFINITE, 0); }
    void broadcast() { WakeAllConditionVariable(&condvar); }
    void signal() { WakeConditionVariable(&condvar); }
private:
    CONDITION_VARIABLE condvar;
};
#else // _WIN32
class ConditionVariable
{
public:
    ConditionVariable() { pthread_cond_init(&cond, 0); }
    ~ConditionVariable() { pthread_cond_destroy(&cond); }
    void wait(Mutex& mutex) { pthread_cond_wait(&cond, &mutex.mutex); }
    void broadcast() { pthread_cond_broadcast(&cond); }
    void signal() { pthread_cond_signal(&cond); }
private:
    pthread_cond_t cond;
};
#endif // _WIN32

#if (defined _WIN32 && !(defined __MINGW32__))
static unsigned __stdcall start_wrapper(void* args);
class Thread
{
public:
    Thread(void* (*start)(void*), void* args = 0) { _start = start; _args = args; handle = (HANDLE)_beginthreadex(0, 0, start_wrapper, this, 0, 0); }
    ~Thread() {}
    void join() { WaitForSingleObject(handle, INFINITE); CloseHandle(handle); }
private:
    friend unsigned __stdcall start_wrapper(void* args)
    {
        Thread* t = (Thread*)args;
        t->_start(t->_args);
        return 0;
    }
    HANDLE handle;
    void* (*_start)(void*);
    void* _args;
};

#else // _WIN32
class Thread
{
public:
    Thread(void* (*start)(void*), void* args = 0) { pthread_create(&t, 0, start, args); }
    ~Thread() {}
    void join() { pthread_join(t, 0); }
private:
    pthread_t t;
};
#endif // _WIN32
#else // NCNN_THREADS
class Mutex
{
public:
    Mutex() {}
    ~Mutex() {}
    void lock() {}
    void unlock() {}
};

class ConditionVariable
{
public:
    ConditionVariable() {}
    ~ConditionVariable() {}
    void wait(Mutex& /*mutex*/) {}
    void broadcast() {}
    void signal() {}
};

class Thread
{
public:
    Thread(void* (*/*start*/)(void*), void* /*args*/ = 0) {}
    ~Thread() {}
    void join() {}
};
#endif // NCNN_THREADS

} // namespace ncnn

#if NCNN_SIMPLESTL
#include "simplestl.h"
#else
#include <vector>
#include <string>
#endif

#if NCNN_STDIO
#if __ANDROID_API__ >= 8
#include <android/log.h>
#define NCNN_LOGE(...) do { \
    fprintf(stderr, ##__VA_ARGS__); fprintf(stderr, "\n"); \
    __android_log_print(ANDROID_LOG_WARN, "ncnn", ##__VA_ARGS__); } while(0)
#else // __ANDROID_API__ >= 8
#include <stdio.h>
#define NCNN_LOGE(...) do { \
    fprintf(stderr, ##__VA_ARGS__); fprintf(stderr, "\n"); } while(0)
#endif // __ANDROID_API__ >= 8
#else
#define NCNN_LOGE(...)
#endif

#endif // NCNN_PLATFORM_H
