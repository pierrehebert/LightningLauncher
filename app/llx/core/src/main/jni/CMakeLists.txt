cmake_minimum_required(VERSION 3.4.1)

add_library( # Specifies the name of the library.
             ll

             # Sets the library as a static library.
             SHARED

             # Provides a relative path to your source file(s).
             ll.c )

#find_library(m -llog -ljnigraphics -landroid)
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(android-lib android)

target_link_libraries(ll ${log-lib} ${jnigraphics-lib} ${android-lib})

set_target_properties(ll PROPERTIES LINKER_LANGUAGE CXX)