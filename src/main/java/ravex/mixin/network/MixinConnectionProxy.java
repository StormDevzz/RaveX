package ravex.mixin.network;

import net.minecraft.network.Connection;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.debugchart.LocalSampleLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
<<<<<<< HEAD
import ravex.proxy.Proxy;
import ravex.proxy.ProxyServer;
=======
import ravex.modules.misc.Proxy;
import ravex.proxy.ProxyNative;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

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

<<<<<<< HEAD
        if (!Proxy.isEnabled()) return;
=======
        Proxy proxy = Proxy.INSTANCE;
        if (!proxy.getEnabled()) return;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

        String targetHost = address.getHostString();
        int targetPort = address.getPort();

<<<<<<< HEAD
        if (!ProxyServer.start(
            Proxy.getType(),
            Proxy.getHost(),
            Proxy.getPort(),
            targetHost,
            targetPort,
            Proxy.hasAuth() ? Proxy.getUsername() : "",
            Proxy.hasAuth() ? Proxy.getPassword() : ""
        )) return;

        int localPort = ProxyServer.getLocalPort();
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
