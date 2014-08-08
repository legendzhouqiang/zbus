################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../remoting/msg.c \
../remoting/net.c \
../remoting/remoting.c 

OBJS += \
./remoting/msg.o \
./remoting/net.o \
./remoting/remoting.o 

C_DEPS += \
./remoting/msg.d \
./remoting/net.d \
./remoting/remoting.d 


# Each subdirectory must supply rules for building sources it contributes
remoting/%.o: ../remoting/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cross GCC Compiler'
	gcc -I../json -I../remoting -I../util -I../zbus -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


