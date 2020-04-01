import ElePay

fileprivate struct RnElepayResult {
    let state: String
    let paymentId: String?

    var asDictionary: Dictionary<String, Any> {
        return [
            "state": state,
            "paymentId": paymentId ?? NSNull()
        ]
    }
}

fileprivate struct RnElepayError {
    let code: String
    let reason: String
    let message: String

    var asDictionary: Dictionary<String, String> {
        return [
            "errorCode": code,
            "reason": reason,
            "message": message
        ]
    }
}

@objc(ElepayCordova)
class ElepayCordova : CDVPlugin {
    @objc(initElepay:)
    func initElepay(command: CDVInvokedUrlCommand) {
        let configs = command.arguments[0] as? Dictionary<String, Any> ?? [:]

        let publicKey = configs["publicKey"] as? String ?? ""
        let apiUrl = configs["apiUrl"] as? String ?? ""
        ElePay.initApp(key: publicKey, apiURLString: apiUrl)

        performChangingLanguage(langConfig: configs)
    }

    @objc(changeLanguage:)
    func changeLanguage(command: CDVInvokedUrlCommand) {
        let langConfig = command.arguments[0] as? Dictionary<String, Any> ?? [:]
        performChangingLanguage(langConfig: langConfig)
    }

    private func performChangingLanguage(langConfig: [String: Any]) {
        let langCodeStr = langConfig["languageKey"] as? String ?? ""
        if let langCode = retrieveLanguageCode(from: langCodeStr) {
            ElePayLocalization.shared.switchLanguage(code: langCode)
        }
    }

    @objc(handleOpenUrl:)
    func handleOpenUrl(command: CDVInvokedUrlCommand) {
        guard let urlStr = command.arguments[0] as? String, let url = URL(string: urlStr) else {
            let error = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: "Invalid url for handling.")
            commandDelegate!.send(error, callbackId: command.callbackId)
            return
        }

        _ = ElePay.handleOpenURL(url)
    }

    @objc(handlePayment:)
    func handlePayment(command: CDVInvokedUrlCommand) {
        guard let payload = command.arguments[0] as? Dictionary<String, Any> else {
            let error = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: "Invalid payment payload.")
            commandDelegate!.send(error, callbackId: command.callbackId)
            return
        }

        let delegate = commandDelegate!
        let callbackId = command.callbackId

        DispatchQueue.main.async { [weak self] in
            self?.handlePaymentInMainThread(payload: payload, delegate: delegate, callbackId: callbackId)
        }
    }

    private func retrieveLanguageCode(from langStr: String) -> ElePayLanguageCode? {
        let ret: ElePayLanguageCode?
        switch (langStr.lowercased()) {
            case "english": ret = .en
            case "simplifiedchinise": ret = .cn
            case "traditionalchinese": ret = .tw
            case "japanese": ret = .ja
            default: ret = nil
        }
        return ret
    }

    @discardableResult
    private func handlePaymentInMainThread(
        payload: [String: Any],
        delegate: CDVCommandDelegate,
        callbackId: String?
    ) -> Bool {
        ElePay.handlePayment(charge: payload, viewController: viewController) { result in
            switch result {
            case .succeeded(let paymentId):
                let res = RnElepayResult(state: "succeeded", paymentId: paymentId).asDictionary
                delegate.send(
                    CDVPluginResult(status: CDVCommandStatus_OK, messageAs: res),
                    callbackId: callbackId)
            case .cancelled(let paymentId):
                let res = RnElepayResult(state: "cancelled", paymentId: paymentId).asDictionary
                delegate.send(
                    CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: res),
                    callbackId: callbackId)
            case .failed(let paymentId, let error):
                let err: RnElepayError
                switch error {
                case .alreadyMakingPayment(_):
                    err = RnElepayError(code: "", reason: "Already making payment", message: "")
                case .invalidPayload(let errorCode, let message):
                    err = RnElepayError(code: errorCode, reason: "Invalid payload", message: message)
                case .paymentFailure(let errorCode, let message):
                    err = RnElepayError(code: errorCode, reason: "Payment failure", message: message)
                case .paymentMethodNotInitialized(let errorCode, let message):
                    err = RnElepayError(code: errorCode, reason: "Payment method not initialized", message: message)
                case .serverError(let errorCode, let message):
                    err = RnElepayError(code: errorCode, reason: "Server error", message: message)
                case .systemError(let errorCode, let message):
                    err = RnElepayError(code: errorCode, reason: "System error", message: message)
                case .unsupportedPaymentMethod(let errorCode, let paymentMethod):
                    err = RnElepayError(code: errorCode, reason: "Unsupported payment method", message: paymentMethod)
                @unknown default:
                    err = RnElepayError(code: "-1", reason: "Undefined reason", message: "Unknonw error")
                    break
                }

                var res = RnElepayResult(state: "failed", paymentId: paymentId).asDictionary
                res["error"] = err.asDictionary
                delegate.send(
                    CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: res),
                    callbackId: callbackId)
            @unknown default:
                break
            }
        }
    }
}
