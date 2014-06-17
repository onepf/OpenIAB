using UnityEngine;
using System.Collections;

public class Bullet : MonoBehaviour
{
    [SerializeField]
    float _speed = 100.0f;

    void Update()
    {
        transform.Translate(0, 0, _speed * Time.deltaTime);
        var pos = Camera.main.WorldToViewportPoint(transform.position);
        if (pos.y > 1.1f)
            Destroy(gameObject);
    }
}
