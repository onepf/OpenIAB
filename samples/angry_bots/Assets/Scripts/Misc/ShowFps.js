
@script ExecuteInEditMode

private var gui : GUIText;

private var updateInterval = 1.0;
private var lastInterval : double; // Last interval end time
private var frames = 0; // Frames over current interval

function Start()
{
    lastInterval = Time.realtimeSinceStartup;
    frames = 0;
}

function OnDisable ()
{
	if (gui)
		DestroyImmediate (gui.gameObject);
}

function Update()
{
    ++frames;
    var timeNow = Time.realtimeSinceStartup;
    if (timeNow > lastInterval + updateInterval)
    {
		if (!gui)
		{
			var go : GameObject = new GameObject("FPS Display", GUIText);
			go.hideFlags = HideFlags.HideAndDontSave;
			go.transform.position = Vector3(0,0,0);
			gui = go.guiText;
			gui.pixelOffset = Vector2(5,55);
		}
        var fps : float = frames / (timeNow - lastInterval);
		var ms : float = 1000.0f / Mathf.Max (fps, 0.00001);
		gui.text = ms.ToString("f1") + "ms " + fps.ToString("f2") + "FPS";
        frames = 0;
        lastInterval = timeNow;
    }
}
