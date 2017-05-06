function MyService() {

}

MyService.prototype.plus = function (a, b) {
    return a + b;
}

MyService.prototype.echo = function (value) {
    return value;
}

MyService.prototype.getString = function (value, c) {
    if (!c) return value + ", frome javascript";
    return value + ", " + c;
}

MyService.prototype.testEncoding = function () {
    return "中文";
}

MyService.prototype.stringArray = function () {
    return ["hong", "leiming"];
}

MyService.prototype.getBin = function () {
    return new Uint8Array(10);
}