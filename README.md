# Wurst Client 7.51.2 — NeoForge порт

Неофициальный порт [Wurst Client 7.51.2](https://github.com/Wurst-Imperium/Wurst7) (Fabric, MC 1.21.1) на **NeoForge 21.1.x**.
Код взят из исходников тега `v7.51.2-MC1.21.1`, а не декомпиляцией jar. Mojang mappings — как в апстриме, переименований нет.

> **Клиентский мод.** На сервер не устанавливать.
>
> ⚠️ **Внимание.** Использование клиентов с читами (в том числе Wurst) запрещено правилами большинства публичных серверов и ведёт к блокировке аккаунта — вплоть до перманентного бана. На серверах с античитом (в том числе с проверкой модов) мод будет обнаружен. Используйте только там, где это разрешено: в одиночной игре, на своём сервере или на серверах, явно допускающих такие клиенты. Ответственность за последствия — на пользователе.

## Требования

| Компонент | Версия |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.267 (собрано и протестировано; заявлено `[21.1.0,)`) |
| Java | 21 |
| Gradle | 9.1.0 (wrapper в комплекте) |
| ModDevGradle | 2.0.148 |

## Установка

1. Установить NeoForge **21.1.x** для Minecraft **1.21.1** (официальный installer с [neoforged.net](https://neoforged.net/)).
2. Запустить игру один раз, чтобы появилась папка `.minecraft/mods` (или `mods` внутри профиля лаунчера).
3. Скачать `Wurst-Client-7.51.2-neoforge.jar` из раздела **Releases** этого репозитория.
4. Положить jar в папку `mods`. Больше ничего не требуется — dependencies у мода нет.
5. Запустить игру, в меню паузы появится кнопка **Wurst Options**.

Мод **клиентский**: устанавливать его на сервер (в `mods` сервера) не нужно и не следует.

## Сборка

```powershell
.\gradlew.bat build
```

Нужен **JDK 21**: либо `JAVA_HOME` указывает на него, либо `java` из JDK 21 доступен в `PATH`.
Пример для Windows:

```powershell
$env:JAVA_HOME="C:\path\to\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build
```

Готовый артефакт: `build\libs\Wurst-Client-7.51.2-neoforge.jar`.

Замечания:
- `gradlew.bat` может вернуть exit code 1 из-за вывода в stderr — ориентируйтесь на строку `BUILD SUCCESSFUL`.
- Первая сборка долгая (скачивание и патчинг Minecraft/NeoForge), последующие — секунды.
- Запуск dev-клиента: `.\gradlew.bat runClient`.

## Архитектура порта

Точка входа — `net.wurstclient.WurstNeoForge` (`@Mod("wurst")`), заменяет Fabric-овский `WurstInitializer`.

Wurst нужно инициализировать **во время конструктора `Minecraft`**, до создания загрузочного экрана, иначе миксины, срабатывающие при отрисовке текста и `Screen.init()`, увидят `null` вместо `HackList`. NeoForge-аналог нужного тайминга — `RegisterClientReloadListenersEvent` (окно, настройки и `ResourceManager` уже есть, загрузочный экран ещё нет). Дополнительно висит `ClientTickEvent.Post` как fallback, от повторного запуска защищает флаг `initialized`.

Замены Fabric API сделаны точечно, без прослойки:

| Fabric API | Замена |
|---|---|
| `Screens.getButtons(Screen)` | `net.wurstclient.util.ScreenUtils.getButtons(Screen)` |
| `FabricLoader.getInstance().isModLoaded(...)` / `.getModContainer(...)` | `ModList.get()` / `FMLEnvironment` |
| `ClientLoginConnectionEvents.INIT` | `ClientPlayerNetworkEvent.LoggingIn` |
| `fabric.mod.json` | `META-INF/neoforge.mods.toml` |
| `wurst.accesswidener` | `META-INF/accesstransformer.cfg` |
| миксины `indigo/*`, `sodium/*` | удалены (нет соответствующих модов) |

## Отличия от апстрима

### Добавленные файлы
- `src/main/java/net/wurstclient/WurstNeoForge.java` — точка входа NeoForge.
- `src/main/java/net/wurstclient/util/ScreenUtils.java` — замена `Screens.getButtons`.
- `src/main/resources/META-INF/neoforge.mods.toml` — метаданные мода.
- `src/main/resources/META-INF/accesstransformer.cfg` — AT вместо access widener.
- `src/main/resources/pack.mcmeta` — нужно, чтобы ресурсы мода считались паками.

### Удалённые файлы
- `WurstInitializer.java`, `fabric.mod.json`, `wurst.accesswidener`;
- `mixin/indigo/*`, `mixin/sodium/*`;
- `src/main/java/net/wurstclient/test/*`, `src/test/*` — тесты требовали Fabric GameTest API.

### Изменённые файлы (симптом → правка)

| Файл | Причина |
|---|---|
| `WurstNeoForge.java` (новый) | инициализация в `RegisterClientReloadListenersEvent`, иначе краш на загрузочном экране |
| `util/ScreenUtils.java` (новый) | `Screens.getButtons` из Fabric API |
| `mixin/GameMenuScreenMixin.java` | `buttons.add(...)` по неизменяемому списку → `addRenderableWidget(...)` |
| `mixin/TitleScreenMixin.java`, `AltManagerScreen.java`, `NavigatorFeatureScreen.java`, `NavigatorNewKeybindScreen.java`, `NavigatorRemoveKeybindScreen.java`, `WurstOptionsScreen.java`, `serverfinder/CleanUpScreen.java` | перевод на `ScreenUtils.getButtons` и `addRenderableWidget` |
| `mixin/BlockModelRendererMixin.java` | цель инъекции смещена на 12-арг `tesselateWithAO` / `tesselateWithoutAO` (с `ModelData` / `RenderType`) |
| `mixin/DownloaderMixin.java` | переименованная лямбда `method_55485` → `lambda$runDownload$0`, `remap = false` |
| `mixin/LanguageManagerMixin.java` | событие перезагрузки ресурсов под NeoForge |
| `WurstTranslator.java` | проверка «своего» ресурспака: NeoForge использует `KnownPack("neoforge", "mod/wurst")`, а не `("fabric", "wurst")` — без этого игнорировались все переводы Wurst |
| `util/chunk/ChunkSearcher.java` | **баг апстрима**: фоновое чтение чанка гонялось с выгрузкой чанка → `MissingPaletteEntryException` и краш. Обход обёрнут в `try/catch`, `getMatches()`/`getMatchesList()` не пробрасывают исключение из `join()` |
| `hacks/NewChunksHack.java` | тот же класс гонки в фоновом `checkLoadedChunk()`, обёрнут в `try/catch` |
| `hacks/XRayHack.java` | правка под NeoForge (типы/доступ к `RenderType`) |
| `analytics/PlausibleAnalytics.java`, `other_features/NoChatReportsOtf.java` | определение загрузчика/мода без Fabric API |
| `wurst.mixins.json` | убраны indigo/sodium, добавлены neo-миксины, `defaultRequire: 0` |
| `build.gradle`, `gradle.properties`, `settings.gradle` | ModDevGradle вместо Loom |

## Диагностика

- `wurst.mixins.json` содержит `"injectors": { "defaultRequire": 0 }`. Это значит, что **неприменившаяся инъекция не роняет игру**, а лишь отключает соответствующую функцию. Так проще жить с расхождениями NeoForge-патчей.
- Проверить, что именно не применилось: `logs\latest.log`, строки с `Mixin` / `@Inject` / именем миксина.
- Для отладки конкретного миксина можно временно поставить `defaultRequire: 1` и запустить — неподходящая инъекция даст явный краш с указанием метода-цели.
- Краши NeoForge пишет в `crash-*.txt` в корне игры, там же `logs\latest.log`.

## Телеметрия (Plausible Analytics)

В Wurst встроена анонимная статистика через [Plausible](https://plausible.io/) — она **включена по умолчанию**. Реализация: `src/main/java/net/wurstclient/analytics/PlausibleAnalytics.java`.

**Куда уходит:** `https://plausible.wurstclient.net/api/event` — это собственный self-hosted сервер Plausible автора Wurst (Германия, EU). Порт эту настройку не менял: данные идут апстриму, а не сборщику порта.

**Что отправляется** (HTTP POST, JSON: `name`, `url`, `domain`, `props`):
- события: `pageview /` при запуске клиента и `pageview /in-game` при входе в мир;
- версия Wurst (полная и короткая), версия Minecraft, версия NeoForge;
- версии ModMenu, Sodium и Sinytra Connector, если они установлены;
- язык игры (`language`) и тип игры (`game_type`: `singleplayer`, `lan`, `realms`, `multiplayer`);
- HTTP-заголовок `User-Agent` — строка вида `Windows 11 (amd64) version 10.0`, как в краш-репортах.

**Чего не отправляется:** ник, UUID, e-mail, IP-адреса с привязкой к пользователю, содержимое чата, список серверов, координаты, никаких cookies и постоянных идентификаторов. Plausible не хранит персональные данные по своей модели.

**Как отключить:** кнопка **Wurst Options → Count Users: OFF**. Состояние пишется в `wurst/analytics.json` (`{"version":2,"enabled":false}`) и переживает перезапуск. В dev-режиме (`!FMLEnvironment.production`) события вообще не отправляются, только логируются.

## Обновление до новой версии Wurst

1. Взять исходники нового тега из `Wurst7` (Mojmap).
2. Перенести файлы из списка «Изменённые» (таблица выше) — по сути это и есть весь порт.
3. Заново сконвертировать `wurst.accesswidener` → `META-INF/accesstransformer.cfg` (формат: `public net.minecraft.… <field|method>`).
4. Проверить `build.gradle` / `gradle.properties` на актуальные версии NeoForge.
5. `gradlew.bat build`, затем тест: загрузка, одиночный мир, Esc → Wurst Options, включение пары хаков, `Search`/`CaveFinder` (они используют `ChunkSearcher`).

## Структура

```
wurst-neoforge/
├─ build.gradle              # ModDevGradle, NeoForge 21.1.267
├─ gradle.properties         # версии MC / NeoForge / мода
├─ settings.gradle
├─ gradle/wrapper/           # Gradle 9.1.0
└─ src/main/
   ├─ java/net/wurstclient/  # исходники Wurst + WurstNeoForge + ScreenUtils
   └─ resources/
      ├─ META-INF/           # neoforge.mods.toml, accesstransformer.cfg
      ├─ assets/wurst/       # текстуры, translations/*.json
      ├─ wurst.mixins.json
      └─ pack.mcmeta
```

## Лицензия

GPL-3.0-or-later, как у оригинального Wurst Client (`LICENSE.txt`). Автор оригинала — Alexander01998 и контрибьюторы Wurst-Imperium. Этот порт — неофициальный, к апстриму отношения не имеет.
