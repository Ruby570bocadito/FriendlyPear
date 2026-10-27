package app.friendlypear.core.agent

/**
 * Apps that let someone else see and control the phone. Fake bank and support callers ask people to install
 * them while they stay on the line ("vishing"): no real bank ever does.
 */
object RemoteApps {
    private val packages = mapOf(
        "com.anydesk.anydeskandroid" to "AnyDesk",
        "com.teamviewer.quicksupport.market" to "TeamViewer QuickSupport",
        "com.teamviewer.host.market" to "TeamViewer Host",
        "com.teamviewer.teamviewer.market.mobile" to "TeamViewer",
        "com.rustdesk.rustdesk" to "RustDesk",
        "com.carriez.flutter_hbb" to "RustDesk",
        "com.sand.airdroid" to "AirDroid",
        "com.sand.airdroidbiz" to "AirDroid Business",
        "com.sand.airmirror" to "AirMirror",
        "com.splashtop.sos" to "Splashtop SOS",
        "com.splashtop.streamer.addon.knox" to "Splashtop",
        "com.zoho.assist.agent" to "Zoho Assist",
        "com.logmein.rescuemobile" to "LogMeIn Rescue",
        "com.realvnc.server" to "VNC Server",
        "com.supremocontrol.supremo" to "Supremo",
    )

    /** All the packages, so the phone can be allowed to see when one of them is installed. */
    val ALL: Set<String> get() = packages.keys

    /** The app's usual name if [packageName] is a remote-control app, null otherwise. */
    fun nameOf(packageName: String): String? = packages[packageName]
}
