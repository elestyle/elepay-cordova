var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'ElepayCordova', 'coolMethod', [arg0]);
};

exports.initElepay = function (configs) {
    exec(function (success) { },
        function (error) { },
        'ElepayCordova',
        'initElepay',
        [configs]);
}

exports.changeLanguage = function (langConfig) {
    exec(function (success) { },
        function (error) { },
        'ElepayCordova',
        'changeLanguage',
        [langConfig]);
}

exports.handleOpenUrl = function (url) {
    exec(function (success) { },
        function (error) { },
        'ElepayCordova',
        'handleOpenUrl',
        [url]);
}

exports.handlePayment = function (chargeObject, success, error) {
    exec(success, error, 'ElepayCordova', 'handlePayment', [chargeObject]);
}
