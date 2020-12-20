package older.v06.ranger.stream.maps.light;

import model.PlayerView;
import older.v06.ranger.stream.collections.AllEntities;
import older.v06.ranger.stream.maps.EntitiesMap;

public class SimCityPlan {

    private int[][] freeSizeX;
    private int[][] freeSizeY;
    private int[][] freeSize;
    private int[][] emptySizeX;
    private int[][] emptySizeY;
    private int[][] emptySize;
    private int mapSize;

    public int getPassableSize(int x, int y) {
        return freeSize[x][y];
    }

    public int getEmptySize(int x, int y) {
        return emptySize[x][y];
    }

    public void init(PlayerView playerView, EntitiesMap entitiesMap, AllEntities allEntities, WarMap warMap, VirtualResources resources) {
        warMap.checkTick(playerView);
        mapSize = playerView.getMapSize();

        if (freeSize == null) {
            freeSizeX = new int[mapSize][mapSize];
            freeSizeY = new int[mapSize][mapSize];
            freeSize = new int[mapSize][mapSize];

            emptySizeX = new int[mapSize][mapSize];
            emptySizeY = new int[mapSize][mapSize];
            emptySize = new int[mapSize][mapSize];
        }

        for (int i = mapSize - 1; i >= 0; i--) {
            for (int j = mapSize - 1; j >= 0; j--) {
                if (entitiesMap.isPassable(i, j) && resources.getResourceCount(i, j) == 0) {
                    int prevFreeX = (i + 1 < mapSize) ? freeSizeX[i + 1][j] : 0;
                    int prevFreeY = (j + 1 < mapSize) ? freeSizeY[i][j + 1] : 0;
                    int prevFree = (i + 1 < mapSize) && (j + 1 < mapSize) ? freeSize[i + 1][j + 1] : 0;
                    freeSizeX[i][j] = 1 + prevFreeX;
                    freeSizeY[i][j] = 1 + prevFreeY;
                    freeSize[i][j] = 1 + Math.min(prevFreeX, Math.min(prevFreeY, prevFree));
//                    DebugInterface.print(freeSize[i][j], i, j);
                } else {
                    freeSizeX[i][j] = 0;
                    freeSizeY[i][j] = 0;
                    freeSize[i][j] = 0;
                }
                if (entitiesMap.isEmpty(i, j) && resources.getResourceCount(i, j) == 0) {
                    int prevFreeX = (i + 1 < mapSize) ? emptySizeX[i + 1][j] : 0;
                    int prevFreeY = (j + 1 < mapSize) ? emptySizeY[i][j + 1] : 0;
                    int prevFree = (i + 1 < mapSize) && (j + 1 < mapSize) ? emptySize[i + 1][j + 1] : 0;
                    emptySizeX[i][j] = 1 + prevFreeX;
                    emptySizeY[i][j] = 1 + prevFreeY;
                    emptySize[i][j] = 1 + Math.min(prevFreeX, Math.min(prevFreeY, prevFree));
//                    DebugInterface.print(emptySize[i][j], i, j);
                } else {
                    emptySizeX[i][j] = 0;
                    emptySizeY[i][j] = 0;
                    emptySize[i][j] = 0;
                }
            }
        }
    }
}
