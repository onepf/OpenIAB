// C#
// Creates a simple wizard that lets you create a Ligth GameObject
// or if the user clicks in "Apply", it will set the color of the currently
// object selected to red

using UnityEditor;
using UnityEngine;

class ReflectionThreshholdTweaker : ScriptableWizard {
    
    [MenuItem ("Tools/Tweak reflection mask")]
    static void CreateWizard () {
        ScriptableWizard.DisplayWizard<ReflectionThreshholdTweaker>("Tweak reflection masks", "Create", "Apply");
        //If you don't want to use the secondary button simply leave it out:
        //ScriptableWizard.DisplayWizard<WizardCreateLight>("Create Light", "Create");
        
    }
    void OnWizardCreate () {
        
    }  
    void OnWizardUpdate () {
    }   

	//void OnGUI() {
    //	GUILayout.Label("Hit apply to make all uber shader textures put grayscale into alpha.");
	//	
	//}

    // When the user pressed the "Apply" button OnWizardOtherButton is called.
    void OnWizardOtherButton () {
       	
       	MeshRenderer[] objs = FindObjectsOfType(typeof(MeshRenderer)) as 	MeshRenderer[];
       	Debug.Log("objs.Length "+objs.Length);
       	
       	Shader s = Shader.Find("DualStick/UberShader");
       	
       	if(objs.Length>0)
       	foreach( MeshRenderer go in objs) {
       		Debug.Log("go.sharedMaterial.shader.name "+go.sharedMaterial.shader.name);
       		if(go.sharedMaterial.shader == s) {
       			Debug.Log(go.name);
       			
            string path = AssetDatabase.GetAssetPath(go.renderer.sharedMaterial.GetTexture("_MainTex")); 
            TextureImporter textureImporter = AssetImporter.GetAtPath(path) as TextureImporter; 
            
            Debug.Log("adjusting @ "+path);
            textureImporter.grayscaleToAlpha = true;  

            AssetDatabase.ImportAsset(path);  
       		}
       	}       	
         	
       // }
                
    }
}