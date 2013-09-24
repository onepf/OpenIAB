using UnityEngine;
using UnityEditor;
using System.Collections;
 
public class TransformCopier : ScriptableObject
{
    private static Vector3 position;
    private static Quaternion rotation;
    //private static Vector3 scale;
    
    [MenuItem ("Tools/Transform Copier/Copy Transform &%c")]
    static void DoRecord()
    {
       position = Selection.activeTransform.localPosition;
       rotation = Selection.activeTransform.localRotation;
       //scale = Selection.activeTransform.localScale;
        
//        EditorUtility.DisplayDialog("Transform Copy", "Local position, rotation, & scale of "+myName +" copied relative to parent.", "OK", "");
    }
 
    [MenuItem ("Tools/Transform Copier/Paste Transform &%v")]
    static void DoApply()
    {
        Selection.activeTransform.localPosition = position;
        Selection.activeTransform.localRotation = rotation;
        //Selection.activeTransform.localScale = scale;      
        
//        EditorUtility.DisplayDialog("Transform Paste", "Local position, rotation, and scale of "+myName +"  pasted relative to parent of "+Selection.activeTransform.name+".", "OK", "");
    }
}
 