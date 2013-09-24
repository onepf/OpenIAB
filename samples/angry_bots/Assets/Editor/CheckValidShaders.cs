using UnityEngine;
using UnityEditor;
using System.Collections;
 
public class CheckValidShaders : ScriptableObject
{
    [MenuItem ("Tools/Check valid shaders")]
    static void DoRecord()
    {
       GameObject[] objs = Selection.gameObjects;
       foreach (GameObject obj in objs) {
       		if (obj.GetComponent<MeshRenderer> ()) {
       			MeshRenderer render = obj.GetComponent<MeshRenderer> ();
       			if(render.sharedMaterial.shader) {
       				if(!render.sharedMaterial.shader.name.Contains("Dualstick"))
       					Debug.Log ("weird shader " + render.sharedMaterial.shader.name + " @ " + obj.name, obj.transform);	
       			}
       		}
       }
    }
}
 