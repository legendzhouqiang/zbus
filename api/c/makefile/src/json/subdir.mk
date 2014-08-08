################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/json/json.c 

OBJS += \
./src/json/json.o 

C_DEPS += \
./src/json/json.d 


# Each subdirectory must supply rules for building sources it contributes
src/json/%.o: ../src/json/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cross GCC Compiler'
	gcc -I../src/json -I../src/remoting -I../src/util -I../src/zbus -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


