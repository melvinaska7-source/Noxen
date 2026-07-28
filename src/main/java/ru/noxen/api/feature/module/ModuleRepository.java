package ru.noxen.api.feature.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.noxen.implement.features.modules.combat.*;
import ru.noxen.implement.features.modules.misc.*;
import ru.noxen.implement.features.modules.movement.*;
import ru.noxen.implement.features.modules.player.*;
import ru.noxen.implement.features.modules.render.*;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<Module> modules = new ArrayList<>();

    public void setup() {
        register(
                new ClickAction(),
                new ItemTweaks(),
                new Hud(),
                new AuctionHelper(),
                new ProjectilePrediction(),
                new AutoSwap(),
                new NoFriendDamage(),
                new AutoSprint(),
                new ElytraHelper(),
                new AutoRespawn(),
                new AutoTool(),
                new HandTweaks(),
                new BlockHighLight(),
                new AutoTotem(),
                new AutoTpAccept(),
                new Arrows(),
                new AutoLeave(),
                new WorldTweaks(),
                new NoRender(),
                new NameProtect(),
                new AutoArmor(),
                new AutoUse(),
                new NoInteract(),
                new CrossHair(),
                new ServerRPSpoofer(),
                new TargetESP(),
                new ChinaHat(),
                new JumpCircle()
        );
    }

    @Compile
    public void register(Module... module) {
        modules.addAll(List.of(module));
    }

    public List<Module> modules() {
        return modules;
    }
}
