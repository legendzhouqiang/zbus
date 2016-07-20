################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../zbus/src/msg.c \
../zbus/src/remoting.c \
../zbus/src/zbus.c 

OBJS += \
./zbus/src/msg.o \
./zbus/src/remoting.o \
./zbus/src/zbus.o 

C_DEPS += \
./zbus/src/msg.d \
./zbus/src/remoting.d \
./zbus/src/zbus.d 


# Each subdirectory must supply rules for building sources it contributes
zbus/src/%.o: ../zbus/src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	gcc -I"/apps/workspace/zbus-api-c/zbus/include" -I"/apps/workspace/zbus-api-c/zbox/include" -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


