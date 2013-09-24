using UnityEngine;
using UnityEditor;
using System.Collections;

public class TransformGroup : ScriptableObject
{
    [MenuItem ("Tools/Standard Editor Tools/Transform Utilities/Grouping/Group Selection %g", false, 1)]
    static void MenuInsertParent()
    {
        Transform[] transforms = Selection.GetTransforms(SelectionMode.TopLevel |
            SelectionMode.OnlyUserModifiable);

        GameObject newParent = new GameObject("_New Group");
        Transform newParentTransform = newParent.transform;

        if(transforms.Length == 1)
        {
            Transform originalParent = transforms[0].parent;
            transforms[0].parent = newParentTransform;
            if(originalParent)
                newParentTransform.parent = originalParent;
        }
        
        else
        {
            foreach(Transform transform in transforms)
                transform.parent = newParentTransform;
        }
    }
}