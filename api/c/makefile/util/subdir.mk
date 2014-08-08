################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../util/hash.c \
../util/list.c \
../util/log.c \
../util/thread.c 

OBJS += \
./util/hash.o \
./util/list.o \
./util/log.o \
./util/thread.o 

C_DEPS += \
./util/hash.d \
./util/list.d \
./util/log.d \
./util/thread.d 


# Each subdirectory must supply rules for building sources it contributes
util/%.o: ../util/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cross GCC Compiler'
	gcc -I../json -I../remoting -I../util -I../zbus -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


