
using UnityEditor;
using UnityEngine;

class CopyMoodBox : ScriptableWizard {
    
    static MoodBoxData data;
    
    [MenuItem ("Tools/CopyMoodBox")]
    static void Copy () {
    	if (!Selection.activeGameObject)
    		return;
    	data = ((MoodBox)Selection.activeGameObject.GetComponent<MoodBox>()).data;
    }

    [MenuItem ("Tools/PasteMoodBox")]    
    static void Paste () {
    	if (0==Selection.gameObjects.Length)
    		return;    	
    	
    	MoodBoxData copyHere;
    	int i = 0;
    	
    	foreach (GameObject obj in Selection.gameObjects) {
    		if (obj.GetComponent<MoodBox>()) {
	    	copyHere = ((MoodBox)obj.GetComponent<MoodBox>()).data;
	    
	    	copyHere.noiseAmount = data.noiseAmount;
	    	copyHere.colorMixBlend = data.colorMixBlend;
	    	copyHere.colorMix = data.colorMix;
	    	copyHere.fogY = data.fogY;
	    	copyHere.fogColor = data.fogColor;
	    	
	    	i++;
    		}
    	}
    	
    	Debug.Log ("Mood Box pasted " + i + " times.");
    }
}