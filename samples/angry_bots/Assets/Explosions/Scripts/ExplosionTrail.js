
private var _dir : Vector3;

function OnEnable () 
{
	_dir = Random.onUnitSphere;
	_dir.y = 1.25;
}

function Update () 
{
	transform.position += _dir * Time.deltaTime * 5.5;
	
	_dir.y -= Time.deltaTime;
	
	if(_dir.y<0.0 && transform.position.y <= -1.0) {
		this.enabled = false;	
	}
}

