################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../zbus/zbus.c 

OBJS += \
./zbus/zbus.o 

C_DEPS += \
./zbus/zbus.d 


# Each subdirectory must supply rules for building sources it contributes
zbus/%.o: ../zbus/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cross GCC Compiler'
	gcc -I../json -I../remoting -I../util -I../zbus -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


