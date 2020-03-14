function main() {
    var a = {
        age: 10,
        name: "Person A"
    };
    var b = {
        age: 32,
        name: "Person B"
    };
    
    $exampleService.sumPerson(a, b, function (result) {
        alert("Total: age = " + result.age + ", title = " + result.name);
    });
}

Dom.registerEvent(window, "load", main);