package ravex.mixin.network;

import net.minecraft.network.Connection;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.debugchart.LocalSampleLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.misc.Proxy;
import ravex.utility.network.ProxyNative;

import java.net.InetSocketAddress;

@Mixin(Connection.class)
public class MixinConnectionProxy {

    private static final ThreadLocal<Boolean> recursing = ThreadLocal.withInitial(() -> false);

    @Inject(method = "connectToServer", at = @At("HEAD"), cancellable = true)
    private static void onConnectToServer(InetSocketAddress address,
                                           EventLoopGroupHolder groupHolder,
                                           LocalSampleLogger logger,
                                           CallbackInfoReturnable<Connection> ci) {
        if (recursing.get()) return;

        Proxy proxy = Proxy.INSTANCE;
        if (!proxy.getEnabled()) return;

        String targetHost = address.getHostString();
        int targetPort = address.getPort();

        if (!ProxyNative.start(
            proxy.getType(),
            proxy.getHost(),
            proxy.getPort(),
            targetHost,
            targetPort,
            proxy.hasAuth() ? proxy.getUsername() : "",
            proxy.hasAuth() ? proxy.getPassword() : ""
        )) return;

        int localPort = ProxyNative.getLocalPort();
        InetSocketAddress proxyAddr = new InetSocketAddress("127.0.0.1", localPort);

        recursing.set(true);
        try {
            Connection connection = Connection.connectToServer(proxyAddr, groupHolder, logger);
            ci.setReturnValue(connection);
        } finally {
            recursing.set(false);
        }
    }
}
