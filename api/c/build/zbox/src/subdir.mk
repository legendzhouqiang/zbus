################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../zbox/src/blockq.c \
../zbox/src/buffer.c \
../zbox/src/hash.c \
../zbox/src/json.c \
../zbox/src/list.c \
../zbox/src/log.c \
../zbox/src/net.c \
../zbox/src/thread.c 

OBJS += \
./zbox/src/blockq.o \
./zbox/src/buffer.o \
./zbox/src/hash.o \
./zbox/src/json.o \
./zbox/src/list.o \
./zbox/src/log.o \
./zbox/src/net.o \
./zbox/src/thread.o 

C_DEPS += \
./zbox/src/blockq.d \
./zbox/src/buffer.d \
./zbox/src/hash.d \
./zbox/src/json.d \
./zbox/src/list.d \
./zbox/src/log.d \
./zbox/src/net.d \
./zbox/src/thread.d 


# Each subdirectory must supply rules for building sources it contributes
zbox/src/%.o: ../zbox/src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	gcc -I"/apps/workspace/zbus-api-c/zbus/include" -I"/apps/workspace/zbus-api-c/zbox/include" -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


