import { 
    NativeModules,
    Image
} from 'react-native';

module.exports = {
    setWallpaper: (source,callback = (res)=>{}) =>{
        NativeModules.WallPaperManager.setWallpaper(Image.resolveAssetSource(source),callback);
    }
} ;