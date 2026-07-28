package ru.noxen.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.CompileBytecode;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.noxen.api.file.exception.FileProcessingException;
import ru.noxen.api.repository.way.WayRepository;
import ru.noxen.api.system.discord.DiscordManager;
import ru.noxen.api.feature.draggable.DraggableRepository;
import ru.noxen.api.file.*;
import ru.noxen.api.repository.macro.MacroRepository;
import ru.noxen.api.event.EventManager;
import ru.noxen.api.feature.module.ModuleProvider;
import ru.noxen.api.feature.module.ModuleRepository;
import ru.noxen.api.feature.module.ModuleSwitcher;
import ru.noxen.api.system.sound.SoundManager;
import ru.noxen.common.util.logger.LoggerUtil;
import ru.noxen.common.util.render.ScissorManager;
import ru.noxen.core.client.ClientInfo;
import ru.noxen.core.client.ClientInfoProvider;
import ru.noxen.core.listener.ListenerRepository;
import ru.noxen.implement.features.commands.CommandDispatcher;
import ru.noxen.implement.features.commands.manager.CommandRepository;
import ru.noxen.implement.screens.menu.MenuScreen;

import java.io.File;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Main implements ModInitializer {

    @Getter
    static Main instance;
    EventManager eventManager = new EventManager();
    ModuleRepository moduleRepository;
    ModuleSwitcher moduleSwitcher;
    CommandRepository commandRepository;
    CommandDispatcher commandDispatcher;
    MacroRepository macroRepository = new MacroRepository(eventManager);
    WayRepository wayRepository = new WayRepository(eventManager);
    ModuleProvider moduleProvider;
    DraggableRepository draggableRepository;
    DiscordManager discordManager;
    FileRepository fileRepository;
    FileController fileController;
    ScissorManager scissorManager = new ScissorManager();
    ClientInfoProvider clientInfoProvider;
    ListenerRepository listenerRepository;
    boolean initialized;

    @Override
    @CompileBytecode
    public void onInitialize() {
        instance = this;

        initClientInfoProvider();
        initModules();
        initDraggable();
        initFileManager();
        initCommands();
        initListeners();
        initDiscordRPC();
        SoundManager.init();
        MenuScreen menuScreen = new MenuScreen();
        menuScreen.initialize();

        initialized = true;
    }

    @Compile
    @Initialization
    private void initDraggable() {
        draggableRepository = new DraggableRepository();
        draggableRepository.setup();
    }

    @Compile
    @Initialization
    private void initModules() {
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
    }

    @Compile
    @Initialization
    private void initCommands() {
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher(eventManager);
    }


    private void initDiscordRPC() {
        discordManager = new DiscordManager();
        discordManager.init();
    }


    private void initClientInfoProvider() {
        File clientDirectory = new File(MinecraftClient.getInstance().runDirectory, "\\noxen\\");
        File filesDirectory = new File(clientDirectory, "\\files\\");
        File moduleFilesDirectory = new File(filesDirectory, "\\config\\");
        clientInfoProvider = new ClientInfo("Noxen Visuals", "FABOS", "ADMIN", clientDirectory, filesDirectory, moduleFilesDirectory);
    }

    private void initFileManager() {
        DirectoryCreator directoryCreator = new DirectoryCreator();
        directoryCreator.createDirectories(clientInfoProvider.clientDir(), clientInfoProvider.filesDir(), clientInfoProvider.configsDir());
        fileRepository = new FileRepository();
        fileRepository.setup(this);
        fileController = new FileController(fileRepository.getClientFiles(), clientInfoProvider.filesDir(), clientInfoProvider.configsDir());
        try {
            fileController.loadFiles();
        } catch (FileProcessingException e) {
            LoggerUtil.error("Error occurred while loading files: " + e.getMessage() + " " + e.getCause());
        }
    }


    private void initListeners() {
        listenerRepository = new ListenerRepository();
        listenerRepository.setup();
    }
}
