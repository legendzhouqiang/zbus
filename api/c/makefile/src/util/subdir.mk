################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/util/hash.c \
../src/util/list.c \
../src/util/log.c \
../src/util/thread.c 

OBJS += \
./src/util/hash.o \
./src/util/list.o \
./src/util/log.o \
./src/util/thread.o 

C_DEPS += \
./src/util/hash.d \
./src/util/list.d \
./src/util/log.d \
./src/util/thread.d 


# Each subdirectory must supply rules for building sources it contributes
src/util/%.o: ../src/util/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cross GCC Compiler'
	gcc -I../src/json -I../src/remoting -I../src/util -I../src/zbus -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


