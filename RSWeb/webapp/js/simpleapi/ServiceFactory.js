if (typeof(console) == "undefined") {
    window.console = {
            log: function () {},
            trace: function () {}
    }
}
var ServiceFactory = {};

ServiceFactory._serviceConfigs = {};
ServiceFactory.registerService = function(service, def) {
    ServiceFactory._serviceConfigs[service] = def;
    window["$" + service] = ServiceFactory.get(service);
};


function _handleGenericError(e) {
    console.info("Error: " + e);
    throw e;
}
function showProgress(visible, message) {
    if (visible) {
        defaultIndicator.busy(message);
    } else {
        defaultIndicator.done();
    }
}

function objectToXML(object) {
    if (object && object._isSystemLiteral) {
        return object.text;
    }
    if (object == null) {
        return "<null/>";
    }
    if (typeof(object) == "string") {
        return "<string>" + Dom.htmlEncode("" + object, "skipNewLineProcessing") + "</string>";
    }
    if (typeof(object) == "number") {
        return "<int>" + object + "</int>";
    }
    if (typeof(object) == "boolean") {
        return "<boolean>" + object + "</boolean>";
    }
    if (object instanceof Date) {
        return "<date>" + DateUtil.formatForSystem(object) + "</date>";
    }

    var s = "";

    for (var name in object) {
        // check typeof(object) is array object
        if(!object.slice) {
            var clazz = object[name].clazz ? (" class=\"" + object[name].clazz + "\"") : "";
            s += "<" + name + clazz + ">";
            s += objectToXML(object[name]);
            s += "</" + name + ">";
        } else {
            s += objectToXML(object[name]);
        }
    }

    return s;
}
function objectToXMLValue(object) {

    if (object && object._isSystemLiteral) {
        return object.text;
    }
    if (object == null) {
        return "";
    }

    if (object._isSelfSerialized) {
        return object.getBodyXML();
    }

    if (object.valuezz != undefined) {
        return object.valuezz;
    }
    if (typeof(object) == "string") {
        return Dom.htmlEncode("" + object, "skipNewLineProcessing");
    }
    if (typeof(object) == "number") {
        return object;
    }
    if (typeof(object) == "boolean") {
        return object;
    }
    if (object instanceof Date) {
        return  DateUtil.formatForSystem(object);
    }
    var s = "";

    for (var name in object) {
        if (name == "_elementType" || name == "clazz") continue;

        // check typeof(object) is array object
        if (!object.slice) {
            var x = object[name];
            if (x == null) continue;
            var clazz = x && object[name].clazz ? (" class=\"" + object[name].clazz + "\"") : "";
            s += "<" + name + clazz + ">";
            s += objectToXMLValue(object[name]);
            s += "</" + name + ">";
        } else {
            var n = object[0] ? object[0]._elementType : null;
            if (!n && typeof(object[0]) == "number") {
                n = "int";
            }

            s += "<" + n + ">";
            s += objectToXMLValue(object[name]);
            s += "</" + n + ">";
        }
    }

    return s;
}
function _Literal(text) {
    this.text = text;
    this._isSystemLiteral = true;
}
function _List(type, elementType, elements) {
    this.clazz = type;
    this.elementType = elementType;
    this.elements = elements;
    this._isSelfSerialized = true;
}

function _makeStrongTypeList(owner, propertyName, type, elementType) {
    var e = owner[propertyName];
    if (!e) return;

    if (e.elements) return;

    for (var i = 0; i < e.length; i ++) {
        e[i].clazz = elementType;
    }

    owner[propertyName] = new _List(type, elementType, e);
}
_List.prototype.getBodyXML = function () {
    s = "";
    for (var i = 0; i < this.elements.length; i ++) {
        var item = this.elements[i];
        var tag = this.elementType;
        if (this._useConcreteElementTypes && item.clazz) {
            tag = item.clazz;
        }
        if (!tag) tag = "string";

        s += "<" + tag + ">";
        s += objectToXMLValue(item);
        s += "</" + tag + ">";
    }

    return s;
};

function _Map(elementType, obj) {
    this.elementType = elementType || "string";
    this.clazz = "map";
    this.obj = obj;
    this._isSelfSerialized = true;
}

_Map.prototype.getBodyXML = function () {
    s = "";
    for (var name in this.obj) {
        var item = this.obj[name];
        s += "<entry>";
        s += "<" + this.elementType + ">";
        s += objectToXMLValue(name);
        s += "</" + this.elementType + ">";
        s += "<" + this.elementType + ">";
        s += objectToXMLValue(item);
        s += "</" + this.elementType + ">";
        s += "</entry>";
    }

    return s;
};

function _Long(l) {
    return {
        clazz: "java.lang.Long",
        _isSystemLiteral: true,
        text: l
    };
}
function _long(l) {
    return {
        clazz: "long",
        _isSystemLiteral: true,
        text: l
    };
}
function _double(l) {
    return {
        clazz: "double",
        _isSystemLiteral: true,
        text: l
    };
}
function _long_array(numbers) {
    return new _List("long-array", "long", numbers);
}
function _long_list(numbers) {
    return new _List("list", "long", numbers);
}
function _enum(value, clazz) {
    if (value == null) return null;
    return {
        clazz: clazz,
        _isSystemLiteral: true,
        text: value
    }
}
function _date(d) {
    if (d == null) return null;
    return {
        clazz: "date",
        _isSystemLiteral: true,
        text: DateUtil.formatForSystem(d)
    };
}

function pushProgressInfo(info) {

}
function popProgressInfo(info) {

}

var NO_PROGRESS_MESSAGE = "sys:NO_PROGRESS_MESSAGE";
function _invoke(service, entry, args, onCool, onFailed, message) {
//    console.log("Before    " + service + "." + entry + ": ", args);
    args = convertArguments(service, entry, args);
//    console.log("Converted " + service + "." + entry + ": ", args);
    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            if (message) showProgress(false);
            var js = request.responseText;
            try {
                if (request.status != 200) {
                    if (request.status == 401) {
                        console.error("Your session was expired. Please re-login to the system.");
                        //window.top.location.reload();

                        return;
                    }
                    if (onFailed) onFailed(JSON.parse(js));
                    return;
                }

                var response = (js == null || js.length == 0) ? null : JSON.parse(js);

                if (response == null || typeof (response) == "undefined") {
                    onCool(null);
                } else {
                    if (response.error) {
                        if (onFailed) onFailed(response.error);
                    } else {
                        onCool(response.result, args);
                    }
                }


            } catch (e) {
                var trace = printStackTrace({e: e});
                console.log('Error!\n' + 'Message: ' + e.message + '\nStack trace:\n' + trace.join('\n'));
                console.log("Error: message = " + e.message + ", fileName: " + e.fileName + ", lineNumber: " + e.lineNumber);
                if (onFailed) {
                    onFailed(e);
                } else {
                    console.log("request failed: " + service + "." + entry);
                    _handleGenericError(e);
                }
            }
        }
    };

    var argsValues = "";
    if (args != null) {
       for (var i=0; i < args.length ; i++) {
        var argument = args[i];
        var arg = "<vn.evolus.simpleapi.gateway.ServiceArg>";
        if (argument == undefined) {
            arg += "<name>arg" + i + "</name>";
        } else {
            var classzz = getClazz(argument);
            //console.log("find clazz of arg: "  + argument + ": "+ classzz);
            var valuezz = objectToXMLValue(argument);
            arg += "<name>arg" + i + "</name>";
            arg += "<value class=\""+ classzz+ "\">" +  valuezz + "</value>";
        }
        arg += "</vn.evolus.simpleapi.gateway.ServiceArg>"
        argsValues += arg;
       }
    }
    var id = "rq" + Math.round(Math.random() * 1000) + (new Date().getTime());
    var xmlObject = {
        "vn.evolus.simpleapi.gateway.ServiceRequest" : {
            "serviceName" : new _Literal(service),
            "entryName": new _Literal(entry),
            "arguments" : new _Literal(argsValues)
        }
    };

    var xml = objectToXML(xmlObject);
    if (message) showProgress(true, message);

    request.open("POST", API_TRANSPORT_URI, true);
    request.setRequestHeader("Content-type", "application/xml");
    //request.setRequestHeader("Connection", "keep-alive");
    request.setRequestHeader("X-Rio-Client", "JS");
    request.setRequestHeader("X-Rio-Entry", service + "." + entry);
    //console.log("xml:" + xml);
    request.send(xml);
}

function Proxy(name) {
    this._name = name;
}

function getClazz(obj) {
    if (obj == null || typeof(obj) == "undefined") {
        return "";
    }

    if (obj.clazz) {
      return obj.clazz;
    }

    if (typeof(obj) == "number") {
        return "int"
    }
    if (typeof(obj) == "object" && obj.slice && obj.push) {
        return "list"
    }
    if (typeof(obj) == "object" && obj.getTime) {
        return "date"
    }
    return typeof(obj);
}


ServiceFactory._cache = {};
ServiceFactory._create = function (service) {
    var config = ServiceFactory._serviceConfigs[service];
    if (!config) throw "No service defined for " + service;

    var proxy = new Proxy(service);
    for (var i = 0; i < config.entries.length; i ++) {
        var entryName = config.entries[i];
        proxy[entryName] = ServiceFactory._createEntryFunction(entryName);
    }

    return proxy;
}
ServiceFactory._createEntryFunction = function (entryName) {
    return function () {
        var args = [];
        var onFailed = null;
        var onCool = null;
        var message = null;

        var lastArgsPos = 0;

        var len = arguments.length;


        //case #1  (..., function onCool() {}, function onFailed() {}, "message");
        //case #1' (..., function onCool() {}, null,                   "message");
        //which means: all extra params are provided
        if (len >= 3
                && typeof(arguments[len - 3]) == "function"
                && (arguments[len - 2] == null || typeof(arguments[len - 2]) == "function")
                && typeof(arguments[len - 1]) == "string") {
            onCool = arguments[len - 3];
            onFailed = arguments[len - 2];
            message = arguments[len - 1];
            lastArgsPos = len - 4;

        //case #2  (..., function onCool() {}, "message");
        //which means: no onFailed is provided
        } else if (len >= 2
                && (len == 2 || typeof(arguments[len - 3]) != "function")
                && typeof(arguments[len - 2]) == "function"
                && typeof(arguments[len - 1]) == "string") {

            onCool = arguments[len - 2];
            message = arguments[len - 1];
            lastArgsPos = len - 3;

        //case #3  (..., function onCool() {}, function onFailed() {});
        //case #3' (..., function onCool() {}, null                  );
        //which means: no message is provided
        } else if (len >= 2
                && typeof(arguments[len - 2]) == "function"
                && (arguments[len - 1] == null || typeof(arguments[len - 1]) == "function")) {
            onCool = arguments[len - 2];
            onFailed = arguments[len - 1];
            lastArgsPos = len - 3;

        //case #4  (..., function onCool() {});
        //which means: no onFailed and message are provided
        } else if (len >= 1
                && (len == 1 || typeof(arguments[len - 2]) != "function")
                && typeof(arguments[len - 1]) == "function") {

            onCool = arguments[len - 1];
            lastArgsPos = len - 2;
        } else if (len == 0
                || (len > 0 && typeof(arguments[len - 1]) != "function")) {

            onCool = null;
            lastArgsPos = len - 1;
        } else {
            throw "Invalid invocation sigunature.";
        }

        for (var i = 0; i <= lastArgsPos; i ++) {
            args.push(arguments[i]);
        }
        if (!onFailed) {
            onFailed = function (e) {
                if (e && e.message) {
                    console.error(e.message);
                }
            };
        }

        return _invoke(this._name, entryName, args, onCool, onFailed, message);
    }
}

ServiceFactory.get = function (service) {
    if (!ServiceFactory._cache[service]) {
        ServiceFactory._cache[service] = ServiceFactory._create(service);
    }

    return ServiceFactory._cache[service];
}

ServiceFactory.toXmlValue = function (object) {
    return objectToXMLValue(object);
}

var Services = {};

ServiceFactory.buildFriendlyServiceNames = function () {
    for (name in ServiceFactory._serviceConfigs) {
        Services[name] = ServiceFactory.get(name);

        var simpleName = name;
        var target = window;
        if (name.match(/^(.+)\.([^\.]+)$/)) {
            var path = RegExp.$1;
            simpleName = RegExp.$2;

            var parts = path.split(/\./);
            for (var i = 0; i < parts.length; i ++) {
                var part = parts[i];
                if (!target[part]) {
                    target[part] = {};
                }

                target = target[part];
            }
        }

        target[simpleName] = Services[name];
        window[simpleName] = Services[name];
    }
};


function convertArguments(service, entry, args) {
    var types = METHOD_ENTRY_TYPE_MAP[service + "." + entry];
    if (!types) return args;

    var newArgs = [];
    for (var i = 0; i < args.length; i ++) {
        newArgs.push(makeStrongTypeObject(args[i], types[i]));
    }

    return newArgs;
}
function makeStrongTypeObject(obj, type) {
    if (obj == null) return null;
    if (obj._isSystemLiteral || obj._isSelfSerialized || obj.clazz) return obj;

    if (type == "long") return _long(obj);
    if (type == "java.lang.Long") return _Long(obj);
    if (type != null && type.indexOf("java.lang.") == 0) return obj;

    var genericType = null;
    if (type != null && type.match(/^([^#]+)#(.+)$/)) {
        type = RegExp.$1;
        genericType = RegExp.$2;
    }
    if (type != null && type.match(/^java\.util\.(Set|List|Collection)$/)) {
        var elementType = genericType;
        var elements = [];
        for (var i = 0; i < obj.length; i ++) {
            elements.push(makeStrongTypeObject(obj[i], elementType));
        }
        return new _List(type.indexOf(".Set") > 0 ? "set" : "list", elementType, elements);
    }
    if (type != null && type.match(/^java\.util\.(Map)$/)) {
        var elementType = genericType;
        return new _Map(elementType, obj);
    }
    if (type != null && type.match(/^\[L([^;]+);$/)) {
        var elementType = RegExp.$1;
        var elements = [];
        for (var i = 0; i < obj.length; i ++) {
            elements.push(makeStrongTypeObject(obj[i], elementType));
        }
        return new _List(null, elementType, elements);
    }

    if (type != null && type.indexOf("enum:") == 0) {
        return _enum(obj, type.substring(5));
    }

    var fieldMap = FIELD_TYPE_MAP[type];
    if (!fieldMap) return obj;

    var newObj = {};
    for (var name in obj) {
        newObj[name] = obj[name];
    }
    for (var name in fieldMap) {
        var value = newObj[name];
        if (typeof(value) != "undefined") {
            var fieldType = fieldMap[name];
            var hintEntry = type + "@" + name;
            if (TYPE_HINTS[hintEntry]) {
                fieldType = TYPE_HINTS[hintEntry];
            }

            newObj[name] = makeStrongTypeObject(value, fieldType);
        }
    }
    newObj.clazz = type;

    return newObj;
}
function cloneObject(obj) {
    if (obj == null) return null;
    return JSON.parse(JSON.stringify(obj));
}
var TYPE_HINTS = {
};
