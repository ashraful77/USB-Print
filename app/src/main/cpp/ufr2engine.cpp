#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <initializer_list>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "USBPrintUFR2", __VA_ARGS__)

namespace {
using Byte = uint8_t;
using SlimCompFn = int (*)(Byte*, Byte*, int, int, int, int, int*, void*, int, void*);
struct CompParam { Byte xOffset[2]{}; Byte yOffset[2]{}; int8_t zOffset[2]{}; uint16_t farOffset{}; };
struct NativeResult { bool ok; std::vector<Byte> data; std::string message; };
constexpr int W=4958, H=7016;
constexpr CompParam PARAM{{3,9},{6,1},{0,0},80};

jobject result(JNIEnv* e,const NativeResult& r){jclass c=e->FindClass("com/usbprint/app/Ufr2Encoder$Result");if(!c)return nullptr;jmethodID m=e->GetMethodID(c,"<init>","(Z[BLjava/lang/String;)V");if(!m)return nullptr;jbyteArray a=nullptr;if(r.ok){a=e->NewByteArray((jsize)r.data.size());if(!a)return nullptr;if(!r.data.empty())e->SetByteArrayRegion(a,0,(jsize)r.data.size(),reinterpret_cast<const jbyte*>(r.data.data()));}jstring s=e->NewStringUTF(r.message.c_str());jobject o=e->NewObject(c,m,(jboolean)r.ok,a,s);if(a)e->DeleteLocalRef(a);e->DeleteLocalRef(s);return o;}
void p16(std::vector<Byte>&o,int v){o.push_back((Byte)(v>>8));o.push_back((Byte)v);}
void p32le(std::vector<Byte>&o,int v){uint32_t x=(uint32_t)v;o.push_back(x);o.push_back(x>>8);o.push_back(x>>16);o.push_back(x>>24);}
void p32be(std::vector<Byte>&o,int v){uint32_t x=(uint32_t)v;o.push_back(x>>24);o.push_back(x>>16);o.push_back(x>>8);o.push_back(x);}
void ap(std::vector<Byte>&o,std::initializer_list<int>v){for(int x:v)o.push_back((Byte)x);}
std::vector<Byte> job(int d){std::vector<Byte>o;ap(o,{1,0xC1,0x85});p16(o,d);p16(o,d);ap(o,{0xC2,0,0xD8,0x84,0,1,0xDD,0x80,0xC8,0xF0,0x84,8,0,2});return o;}
std::vector<Byte> media(){return {2,0xC3,0x7F,0xF1,0x85,0,0,0,0,0xC5,0,0xC6,0};}
std::vector<Byte> page(){return {3,0xE7,0x85,(Byte)(W>>8),(Byte)W,(Byte)(H>>8),(Byte)H,0xDE,0x80,0,0xC8,0,0xCA,0xA1,0,0,0xCB,0};}
std::vector<Byte> header(int lines,int n){std::vector<Byte>o;ap(o,{0x62,0xE3,0x85});p16(o,W);p16(o,lines);ap(o,{0xE8,0xA5});p16(o,W);p16(o,lines);ap(o,{0xE1,0,0xD7});if(n<=0xFFFF){ap(o,{0x84});p16(o,n);ap(o,{0x9D});p16(o,n);}else{ap(o,{0x88});p32be(o,n);ap(o,{0x9E});p32be(o,n);}return o;}
std::vector<Byte> to2(const jbyte*r){int sb=(W+7)/8,db=(W+3)/4;std::vector<Byte>o((size_t)db*H);for(int y=0;y<H;y++){const Byte*s=(const Byte*)r+(size_t)y*sb;Byte*d=o.data()+(size_t)y*db;for(int x=0;x<W;x++){int v=((s[x>>3]>>(7-(x&7)))&1)?0:3;d[x>>2]|=(Byte)(v<<(6-2*(x&3)));}}return o;}
void cmlp(std::vector<Byte>&o,const Byte*p,size_t n){size_t t=n+6;if(t>0xffff)throw std::runtime_error("CMLP frame too large");o.push_back(1);o.push_back(0x10);o.push_back(t>>8);o.push_back(t);o.push_back(1);o.push_back(0);o.insert(o.end(),p,p+n);}
void frames(std::vector<Byte>&o,const std::vector<Byte>&p){for(size_t i=0;i<p.size();){size_t n=std::min<size_t>(4096,p.size()-i);cmlp(o,p.data()+i,n);i+=n;}static const Byte f[]={8,0,0,0};cmlp(o,f,4);}

NativeResult encode(const jbyte*r,int w,int h,int dpi){
 if(!r||w!=W||h!=H||dpi!=600)return {false,{},"Canon LBP6030B requires 4958x7016 at 600 DPI"};
 void*lib=dlopen("libcanon_slimsfp.so",RTLD_NOW|RTLD_LOCAL);if(!lib){const char*x=dlerror();return {false,{},std::string("Cannot load Canon SLIM library: ")+(x?x:"unknown")};}
 auto comp=reinterpret_cast<SlimCompFn>(dlsym(lib,"lCaptCompEx2"));if(!comp){const char*x=dlerror();dlclose(lib);return {false,{},std::string("Canon SLIM lCaptCompEx2 unavailable: ")+(x?x:"unknown")};}
 try{
  auto input=to2(r);int bpr=(W+3)/4;int cap=(int)std::min<size_t>(input.size()*2ull+4096ull,0x7fffffff);std::vector<Byte>z((size_t)cap);int lines=0;CompParam p=PARAM;
  int n=comp(input.data(),z.data(),bpr,H,cap,2,&lines,&p,2,nullptr);
  LOGE("SLC Ex2 whole-page lines=%d compressed=%d params=03 09 06 01 00 00 50 00 first=%02x %02x %02x %02x %02x %02x %02x %02x",lines,n,z[0],z[1],z[2],z[3],z[4],z[5],z[6],z[7]);
  if(n<=0||n>cap||lines<=0||lines>H){dlclose(lib);return {false,{},"Canon SLIM reference-aware compression returned an invalid result"};}
  std::vector<Byte>pdl;auto add=[&](const std::vector<Byte>&v){pdl.insert(pdl.end(),v.begin(),v.end());};add(job(dpi));add(media());add(std::vector<Byte>{0x51,0xF2,0});add(page());add(std::vector<Byte>{0x61,0xE6,0x80,2,0xE5,0});
  std::vector<Byte>s;s.reserve((size_t)n+18);s.push_back(p.xOffset[0]);s.push_back(p.xOffset[1]);s.push_back(p.yOffset[0]);s.push_back(p.yOffset[1]);s.push_back((Byte)p.zOffset[0]);s.push_back((Byte)p.zOffset[1]);s.push_back((Byte)p.farOffset);s.push_back((Byte)(p.farOffset>>8));s.push_back(1);p32le(s,n+4);s.insert(s.end(),z.begin(),z.begin()+n);ap(s,{0xBD,0x3C,0xDC,0x80,0});add(header(lines,(int)s.size()));add(s);add(std::vector<Byte>{0x13,0x12,0x11});std::vector<Byte>out;out.reserve(pdl.size()+pdl.size()/4096*8+16);frames(out,pdl);dlclose(lib);
  std::ostringstream m;m<<"Canon LBP6030B SFP/SLIM stream: "<<out.size()<<" bytes; PDL "<<pdl.size()<<" bytes; "<<W<<"x"<<H<<" @ 600 DPI; Ex2 whole-page compression; encodedLines="<<lines<<"; compressed="<<n<<"; params=[3,9,6,1,0,0,80]; first8=";for(int i=0;i<8&&i<n;i++){if(i)m<<",";m<<std::hex<<(int)z[i];}return {true,std::move(out),m.str()};
 }catch(const std::exception&e){dlclose(lib);return {false,{},std::string("Native Canon encoder failed: ")+e.what()};}
}
}
extern "C" JNIEXPORT jobject JNICALL Java_com_usbprint_app_NativeUfr2Engine_encodeNative(JNIEnv*e,jobject,jbyteArray a,jint w,jint h,jint dpi,jint,jint){if(!a)return result(e,{false,{},"Raster is null"});jsize n=e->GetArrayLength(a),expected=((w+7)/8)*h;if(n!=expected)return result(e,{false,{},"Unexpected 1-bit raster size"});std::vector<jbyte>r((size_t)n);e->GetByteArrayRegion(a,0,n,r.data());return result(e,encode(r.data(),w,h,dpi));}
