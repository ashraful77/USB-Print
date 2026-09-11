#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cstdint>
#include <initializer_list>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "USBPrintUFR2", __VA_ARGS__)

namespace {
using Byte = uint8_t;
struct NativeResult { bool ok; std::vector<Byte> data; std::string message; };
constexpr int W=4958, H=7016;
// #129: keep the #121 PDL/transport envelope and 256-line bands, but feed
// the HB encoder an 8-bit grayscale raster, matching Canon's SFP/CUPS input.
constexpr int STRIPE_LINES=256;
constexpr size_t CMLP_PAYLOAD=8192;

jobject result(JNIEnv* e,const NativeResult& r){jclass c=e->FindClass("com/usbprint/app/Ufr2Encoder$Result");if(!c)return nullptr;jmethodID m=e->GetMethodID(c,"<init>","(Z[BLjava/lang/String;)V");if(!m)return nullptr;jbyteArray a=nullptr;if(r.ok){a=e->NewByteArray((jsize)r.data.size());if(!a)return nullptr;if(!r.data.empty())e->SetByteArrayRegion(a,0,(jsize)r.data.size(),reinterpret_cast<const jbyte*>(r.data.data()));}jstring s=e->NewStringUTF(r.message.c_str());jobject o=e->NewObject(c,m,(jboolean)r.ok,a,s);if(a)e->DeleteLocalRef(a);e->DeleteLocalRef(s);return o;}
void p16(std::vector<Byte>&o,int v){o.push_back((Byte)(v>>8));o.push_back((Byte)v);}
void p32be(std::vector<Byte>&o,int v){uint32_t x=(uint32_t)v;o.push_back(x>>24);o.push_back(x>>16);o.push_back(x>>8);o.push_back(x);}
void ap(std::vector<Byte>&o,std::initializer_list<int>v){for(int x:v)o.push_back((Byte)x);}
std::vector<Byte> job(int d){std::vector<Byte>o;ap(o,{1,0xC1,0x85});p16(o,d);p16(o,d);ap(o,{0xC2,0,0xD8,0x84,0,1,0xDD,0x80,0xC8,0xF0,0x84,8,0,2});return o;}
std::vector<Byte> media(){return {2,0xC3,0x7F,0xF1,0x85,0,0,0,0,0xC5,0,0xC6,0};}
std::vector<Byte> page(){return {3,0xE7,0x85,(Byte)(W>>8),(Byte)W,(Byte)(H>>8),(Byte)H,0xDE,0x80,0,0xC8,0,0xCA,0xA1,0,0,0xCB,0};}
std::vector<Byte> header(int lines,int n){std::vector<Byte>o;ap(o,{0x62,0xE3,0x85});p16(o,W);p16(o,lines);ap(o,{0xE8,0xA5});p16(o,W);p16(o,lines);ap(o,{0xE1,0,0xD7});if(n<=0xFFFF){ap(o,{0x84});p16(o,n);ap(o,{0x9D});p16(o,n);}else{ap(o,{0x88});p32be(o,n);ap(o,{0x9E});p32be(o,n);}return o;}

// Convert 8-bit grayscale to Canon HB's 2-bit-per-pixel plane. 0 is black,
// 3 is white; the two intermediate codes preserve grayscale information.
std::vector<Byte> to2(const jbyte*r){
 int db=(W+3)/4;
 std::vector<Byte>o((size_t)db*H,0);
 for(int y=0;y<H;y++){
  const Byte*s=reinterpret_cast<const Byte*>(r)+(size_t)y*W;
  Byte*d=o.data()+(size_t)y*db;
  for(int x=0;x<W;x++){
   int g=s[x];
   int v=(g+32)/64;
   if(v>3)v=3;
   d[x>>2]|=(Byte)(v<<(6-2*(x&3)));
  }
 }
 return o;
}
void cmlp(std::vector<Byte>&o,const Byte*p,size_t n){size_t t=n+6;if(t>0xffff)throw std::runtime_error("CMLP frame too large");o.push_back(1);o.push_back(0x10);o.push_back(t>>8);o.push_back(t);o.push_back(1);o.push_back(0);o.insert(o.end(),p,p+n);}
void frames(std::vector<Byte>&o,const std::vector<Byte>&p){for(size_t i=0;i<p.size();){size_t n=std::min(CMLP_PAYLOAD,p.size()-i);cmlp(o,p.data()+i,n);i+=n;}static const Byte f[]={8,0,0,0};cmlp(o,f,4);}

NativeResult encode(const jbyte*r,int w,int h,int dpi){
 if(!r||w!=W||h!=H||dpi!=600)return {false,{},"Canon LBP6030B requires 4958x7016 at 600 DPI"};
 try{
  auto input=to2(r);int bpr=(W+3)/4;
  std::vector<Byte>pdl;pdl.reserve((size_t)H*bpr+4096);
  auto add=[&](const std::vector<Byte>&v){pdl.insert(pdl.end(),v.begin(),v.end());};
  add(job(dpi));add(media());add(std::vector<Byte>{0x51,0xF2,0});add(page());add(std::vector<Byte>{0x61,0xE6,0x80,2,0xE5,0});
  int bands=0;
  for(int lineStart=0;lineStart<H;lineStart+=STRIPE_LINES){
   int lines=std::min(STRIPE_LINES,H-lineStart);int n=bpr*lines;add(header(lines,n));
   size_t start=(size_t)lineStart*bpr;pdl.insert(pdl.end(),input.begin()+start,input.begin()+start+n);bands++;
  }
  add(std::vector<Byte>{0x11,0x12,0x13});
  std::vector<Byte>out;out.reserve(pdl.size()+pdl.size()/CMLP_PAYLOAD*8+16);frames(out,pdl);
  std::ostringstream m;m<<"Canon LBP6030B HB 8-bit GRAYSCALE->2-bit stream: "<<out.size()<<" bytes; PDL "<<pdl.size()<<" bytes; "<<W<<"x"<<H<<" @ 600 DPI; 256-line bands="<<bands<<"; raster8="<<(size_t)W*H<<" bytes; gray quantization=64 levels; CMLP payload=8192; end=11-12-13";
  return {true,std::move(out),m.str()};
 }catch(const std::exception&e){return {false,{},std::string("Native Canon grayscale encoder failed: ")+e.what()};}
}
}
extern "C" JNIEXPORT jobject JNICALL Java_com_usbprint_app_NativeUfr2Engine_encodeNative(JNIEnv*e,jobject,jbyteArray a,jint w,jint h,jint dpi,jint,jint){if(!a)return result(e,{false,{},"Raster is null"});jsize n=e->GetArrayLength(a),expected=(jsize)((size_t)w*h);if(n!=expected)return result(e,{false,{},"Unexpected 8-bit grayscale raster size"});std::vector<jbyte>r((size_t)n);e->GetByteArrayRegion(a,0,n,r.data());return result(e,encode(r.data(),w,h,dpi));}
