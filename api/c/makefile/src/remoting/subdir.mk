################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/remoting/msg.c \
../src/remoting/net.c \
../src/remoting/remoting.c 

OBJS += \
./src/remoting/msg.o \
./src/remoting/net.o \
./src/remoting/remoting.o 

C_DEPS += \
./src/remoting/msg.d \
./src/remoting/net.d \
./src/remoting/remoting.d 


# Each subdirectory must supply rules for building sources it contributes
src/remoting/%.o: ../src/remoting/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cross GCC Compiler'
	gcc -I../src/json -I../src/remoting -I../src/util -I../src/zbus -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


