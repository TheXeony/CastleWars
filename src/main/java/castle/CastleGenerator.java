package castle;

import arc.Events;
import arc.func.Cons;
import arc.math.Mathf;
import arc.util.Log;
import castle.CastleRooms.BlockRoom;
import castle.CastleRooms.MinerRoom;
import castle.CastleRooms.UnitRoom;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.core.GameState.State;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.io.SaveIO;
import mindustry.maps.Map;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.WorldContext;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;
import mindustry.world.blocks.environment.Prop;
import mindustry.world.blocks.environment.TreeBlock;
import mindustry.world.blocks.legacy.LegacyBlock;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

public class CastleGenerator implements Cons<Tiles> {

    public Tiles saved;

    public void loadMap(Map map) {
        try {
            SaveIO.load(map.file, new WorldContext() {
                @Override
                public Tile tile(int index) {
                    return world.tiles.geti(index);
                }

                @Override
                public void resize(int width, int height) {
                    world.resize(width, height);
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID) {
                    Tile tile = new Tile(x, y, floorID, overlayID, wallID);
                    world.tiles.set(x, y, tile);
                    return tile;
                }

                @Override
                public boolean isGenerating() {
                    return world.isGenerating();
                }

                @Override
                public void begin() {
                    beginMapLoad();
                }

                @Override
                public void end() {
                    endMapLoad();
                }
            });

            state.map = map;
        } catch (Throwable e) {
            state.set(State.menu);
            Log.err(e);
        }
    }

    @Override
    public void get(Tiles tiles) {
        saved = tiles;

        tiles = world.resize(world.width(), world.height() * 2 + CastleRooms.size * 4 + 11);

        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                tiles.set(x, y, new Tile(x, y, Blocks.space, Blocks.air, Blocks.air));
            }
        }

        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < saved.height; y++) {
                Tile save = saved.getc(x, y);
                tiles.set(x, y, new Tile(x, y, save.floor(), save.overlay() != Blocks.spawn ? save.overlay() : Blocks.air, save.block() instanceof Prop || save.block() instanceof TreeBlock ? save.block() : Blocks.air));
                tiles.set(x, tiles.height - y - 1, new Tile(x, tiles.height - y - 1, save.floor(), save.overlay() != Blocks.spawn ? save.overlay() : Blocks.air, save.block() instanceof Prop || save.block() instanceof TreeBlock ? save.block() : Blocks.air));
            }
        }

        for (Tile save : saved) {
            if (save.isCenter()) {
                if (save.block() instanceof CoreBlock block) {
                    tiles.getc(save.x, save.y).setNet(block, Team.sharded, 0);
                    tiles.getc(save.x, tiles.height - save.y - 1).setNet(block, Team.blue, 0);

                    new BlockRoom(Team.sharded, save.x, save.y, 5000);
                    new BlockRoom(Team.blue, save.x, tiles.height - save.y - 1, 5000);
                } else if (save.build instanceof SorterBuild sorterBuild) {
                    Item item = sorterBuild.config();
                    int cost = 250 + Mathf.ceil(item.hardness == 0 ? item.cost * 500 : item.hardness * 250);
                    new MinerRoom(item, Team.sharded, save.x, save.y, cost);
                    new MinerRoom(item, Team.blue, save.x, tiles.height - save.y - 1, cost);
                } else if (save.overlay() == Blocks.spawn) {
                    CastleRooms.shardedSpawn = tiles.getc(save.x, save.y);
                    CastleRooms.blueSpawn = tiles.getc(save.x, tiles.height - save.y - 1);
                }
            }
        }

        generateShop(9, saved.height + 4);
    }

    public void generateShop(int shopX, int shopY) {
        int distance = CastleRooms.size + 2;

        addUnitRoom(UnitTypes.dagger, 0, shopX, shopY + 2, 60);
        addUnitRoom(UnitTypes.mace, 1, shopX + distance, shopY + 2, 150);
        addUnitRoom(UnitTypes.fortress, 4, shopX + distance * 2, shopY + 2, 500);
        addUnitRoom(UnitTypes.scepter, 20, shopX + distance * 3, shopY + 2, 3000);
        addUnitRoom(UnitTypes.reign, 45, shopX + distance * 4, shopY + 2, 10000);

        addUnitRoom(UnitTypes.crawler, 0, shopX, shopY + 2 + distance * 2, 75);
        addUnitRoom(UnitTypes.atrax, 1, shopX + distance, shopY + 2 + distance * 2, 160);
        addUnitRoom(UnitTypes.spiroct, 4, shopX + distance * 2, shopY + 2 + distance * 2, 500);
        addUnitRoom(UnitTypes.arkyid, 24, shopX + distance * 3, shopY + 2 + distance * 2, 4600);
        addUnitRoom(UnitTypes.toxopid, 50, shopX + distance * 4, shopY + 2 + distance * 2, 12500);

        addUnitRoom(UnitTypes.nova, 0, shopX + distance * 5, shopY + 2, 75);
        addUnitRoom(UnitTypes.pulsar, 1, shopX + distance * 6, shopY + 2, 160);
        addUnitRoom(UnitTypes.quasar, 4, shopX + distance * 7, shopY + 2, 500);
        addUnitRoom(UnitTypes.vela, 25, shopX + distance * 8, shopY + 2, 3750);
        addUnitRoom(UnitTypes.corvus, 55, shopX + distance * 9, shopY + 2, 15000);

        addUnitRoom(UnitTypes.risso, 0, shopX + distance * 5, shopY + 2 + distance * 2, 150);
        addUnitRoom(UnitTypes.minke, 2, shopX + distance * 6, shopY + 2 + distance * 2, 350);
        addUnitRoom(UnitTypes.bryde, 6, shopX + distance * 7, shopY + 2 + distance * 2, 1200);
        addUnitRoom(UnitTypes.sei, 22, shopX + distance * 8, shopY + 2 + distance * 2, 3750);
        addUnitRoom(UnitTypes.omura, 50, shopX + distance * 9, shopY + 2 + distance * 2, 15000);

        addUnitRoom(UnitTypes.retusa, 1, shopX + distance * 10, shopY + 2, 200);
        addUnitRoom(UnitTypes.oxynoe, 3, shopX + distance * 11, shopY + 2, 750);
        addUnitRoom(UnitTypes.cyerce, 8, shopX + distance * 12, shopY + 2, 1600);
        addUnitRoom(UnitTypes.aegires, 24, shopX + distance * 13, shopY + 2, 4800);
        addUnitRoom(UnitTypes.navanax, 70, shopX + distance * 14, shopY + 2, 11000);

        addUnitRoom(UnitTypes.flare, 0, shopX + distance * 10, shopY + 2 + distance * 2, 250);
        addUnitRoom(UnitTypes.horizon, 2, shopX + distance * 11, shopY + 2 + distance * 2, 500);
        addUnitRoom(UnitTypes.zenith, 8, shopX + distance * 12, shopY + 2 + distance * 2, 2000);
        addUnitRoom(UnitTypes.antumbra, 30, shopX + distance * 13, shopY + 2 + distance * 2, 7500);
        addUnitRoom(UnitTypes.eclipse, 55, shopX + distance * 14, shopY + 2 + distance * 2, 15000);
    }

    public void addUnitRoom(UnitType type, int income, int x, int y, int cost) {
        new UnitRoom(type, UnitRoom.UnitRoomType.attack, income, x, y, cost);
        new UnitRoom(type, UnitRoom.UnitRoomType.defend, -income, x, y + CastleRooms.size + 2, cost);
    }

    public void beginMapLoad() {
        world.setGenerating(true);
    }

    public void endMapLoad() {
        get(world.tiles);

        for (Tile tile : world.tiles) {
            if (tile.block() instanceof LegacyBlock legacy) {
                legacy.removeSelf(tile);
                continue;
            }

            if (tile.build != null) tile.build.updateProximity();
        }

        world.addDarkness(world.tiles);

        Groups.resize(-finalWorldBounds, -finalWorldBounds, world.tiles.width * tilesize + finalWorldBounds * 2, world.tiles.height * tilesize + finalWorldBounds * 2);

        world.setGenerating(false);
        Events.fire(new WorldLoadEvent());
    }
}
