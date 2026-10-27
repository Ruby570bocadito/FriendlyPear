package app.friendlypear.core.agent

/**
 * Banking and payment apps. Opening one while an unknown number is on the line is the fake-bank script:
 * "stay on the phone and open your bank app, I'll guide you".
 */
object BankApps {
    private val packages = setOf(
        // Spain
        "com.bbva.bbvacontigo", "es.bancosantander.apps", "es.lacaixa.mobile.android.newwapicon",
        "net.inverline.bancosabadell.officelocator.android", "www.ingdirect.nativeframe", "com.bankinter.launcher",
        "com.unicajabanco.app", "es.openbank.mobile", "com.kutxabank.android", "es.evobanco.bancamovil",
        // United Kingdom
        "com.barclays.android.barclaysmobilebanking", "uk.co.hsbc.hsbcukmobilebanking", "com.grppl.android.shell.CMBlloydsTSB73",
        "com.rbs.mobile.android.natwest", "co.uk.getmondo", "com.starlingbank.android",
        // United States
        "com.chase.sig.android", "com.infonow.bofa", "com.wf.wellsfargomobile",
        // Everywhere
        "com.revolut.revolut", "de.number26.android", "com.paypal.android.p2pmobile",
    )

    val ALL: Set<String> get() = packages

    fun isBank(packageName: String): Boolean = packageName in packages
}
