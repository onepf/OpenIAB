
public var frames : int = 2;
private var _frames : int = 0;

function OnEnable() {
	_frames = 0;	
}

function Update () 
{
	_frames++;
	if(_frames>frames) {
		gameObject.active = false;
	}
}

