<?php
class HelloWorld {
	
	public function sayHelloTo($name, $param2) {
		return 'Hello ' . $name.$param2;
	}
	
}

$reflectionMethod = new ReflectionMethod('HelloWorld', 'sayHelloTo');
echo $reflectionMethod->invoke(new HelloWorld(),'Mike', "hong");
 
$class = "HelloWorld";
$object = new $class();

?>