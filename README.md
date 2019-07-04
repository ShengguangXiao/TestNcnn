# TestNcnn
The android test project of Ncnn

## Procedures to deploy ncnn into an andriod app.
1. Download the ncnn andriod release from https://github.com/Tencent/ncnn/releases.
2. Open andriod studio and create a new project TestNcnn.
3. Add NcnnWrapper.java into TestNcnn\app\src\main\java\com\example\testncnn. This file is the java interface, they should be declared with native decorator.
4. Create an cpp folder in TestNcnn/app/src/main/, copy the include folder in ncnn release package into cpp folder.
5. Create NcnnWrapper.cpp file in TestNcnn/app/src/main/cpp, it is used to implemente the call of ncnn library. The function name need to follow the JNI standard so it can be called by java. Actually after step 3, android studio is smart enough to show in NcnnWrapper.java the functions without corresponding JNI, you can add them in NcnnWrapper.cpp accordingly.
6. Create jniLibs folder under TestNcnn/app/src/main/, copy the arm64-v8a and armeabi-v7a in ncnn release package in to jinLibs.
7. Create assets folder under TestNcnn/app/src/main/, copy the ncnn model and parameter files in to assets folder.
8. Create CMakeLists.txt file in TestNcnn/app folder, it is used to build the ncnn wrapper library.
9. Open build.gradle file under TestNcnn/app folder, add below options in to android{ defaultConfig { ....}}  
   externalNativeBuild {  
            cmake {  
                cppFlags "-std=c++11 -fopenmp"//c++ compile options  
                abiFilters "armeabi-v7a" // mobile phone cpu architecture  
            }  
        }  
        
   Add below options into android {}  
   externalNativeBuild {  
        cmake {  
            path "CMakeLists.txt"  
        }  
    }  
    

10. Now in MainActiviry.java, you can call the wrapper functions defined in NcnnWrapper.java.
