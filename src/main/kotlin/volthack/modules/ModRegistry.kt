package volthack.modules

import volthack.modules.combat.*
import volthack.modules.movement.*
import volthack.modules.world.*
import volthack.modules.render.*
import volthack.modules.misc.*
import volthack.modules.player.*
import volthack.setting.ModuleManager

object ModRegistry {
    private val modules = listOf(
        // Combat
        KillAura,
        MaceSwap,
        WindAura,
        MaceKill,
        Velocity,
        Trigger,
        AutoCrystal,
        OffHand,
        MainHand,
        HitSound,
        AutoBuff,
        BowSpam,
        AutoWeb,

        // Movement
        NoFall,
        FastLadder,
        FastSwim,
        Fly,
        AirJump,
        Jesus,
        Timer,
        Sneak,
        Scaffold,
        LevitCtrl,
        Step,
        FastFall,

        // Render
        FullBright,
        ESP,
        Notifications,
        Hud,
        FreeLook,
        NameTags,
        BlockOutline,
        PotatoMode,
        ViewClip,
        ViewModel,
        Ambience,
        Chams,
        NoBob,
        Spawner,
        Trajectories,
        WorldTweaks,
        NoRender,
        Tracers,
        PopChams,
        BreadCrumbs,

        // World
        AutoFarm,
        AutoMount,
        BoneMeal,
        FakePlayer,
        StashFinder,

        // Player
        ElytraSwap,
        AutoTool,
        AntiHunger,
        AutoReconnect,
        DiscordStatusModule,

        // Misc
        AutoAuth,
        PacketCanceller,
        AutoFish,
        MessageAura,
        AutoSign,
        SourceFiller,
        PingSpoof,
        HandShake,
        PacketLogger,
        Language
    )

    fun register() {
        ModuleManager.register(*modules.toTypedArray())
    }
}
