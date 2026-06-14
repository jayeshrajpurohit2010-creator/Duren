package com.duren.app.core

sealed class DomainError(message: String) : Exception(message) {
    object UsernameTaken : DomainError("That name's already at the fire. Try another.")
    object InvalidCredentials : DomainError("Email or password incorrect.")
    object WeakPassword : DomainError("Make it stronger. Even campfires need protection.")
    object NetworkUnavailable : DomainError("Your embers will glow when you're back online.")
    object NotAuthenticated : DomainError("Sign in to gather at the fire.")
    object EmptyEmber : DomainError("An ember needs a spark. Say something.")
    object TribeNameRequired : DomainError("Give your tribe a name.")
    object TribeCodeNotFound : DomainError("Nothing found in the dark.")
    object MediaUploadFailed : DomainError("That image wouldn't catch. Try another.")
    object Unknown : DomainError("Something went wrong. Try again.")
}
