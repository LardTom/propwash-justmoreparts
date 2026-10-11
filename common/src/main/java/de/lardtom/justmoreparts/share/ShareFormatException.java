package de.lardtom.justmoreparts.share;

final class ShareFormatException extends Exception {
    private final ShareStatus status;

    ShareFormatException(ShareStatus status) {
        super(status.name(), null, false, false);
        this.status = status;
    }

    ShareStatus status() {
        return this.status;
    }
}
