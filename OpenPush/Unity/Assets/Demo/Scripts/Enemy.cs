using UnityEngine;
using System.Collections;

public class Enemy : MonoBehaviour
{
    [SerializeField]
    Transform _explosionPrefab = null;

    [SerializeField]
    Transform _bigExplosionPrefab = null;

    public static event System.Action<Enemy> Killed;

    float _speed = 50.0f;

    void Awake()
    {
        _speed += Random.Range(0, 20.0f);
        transform.Rotate(0, 0, Random.Range(10, 300));
    }

    void Update()
    {
        transform.Translate(0, 0, -_speed * Time.deltaTime);
        var pos = Camera.main.WorldToViewportPoint(transform.position);
        if (pos.y < -0.2f)
            Destroy(gameObject);
    }

    void OnTriggerEnter(Collider other)
    {
        if (other.gameObject.CompareTag(Tag.Bullet))
        {
            Destroy(gameObject);
            Destroy(other.gameObject);
            Instantiate(_explosionPrefab, transform.position, Quaternion.identity);
            if (Killed != null)
                Killed(this);
        }
        else if (other.gameObject.CompareTag(Tag.Player))
        {
            Destroy(gameObject);
            Instantiate(_bigExplosionPrefab, other.transform.position, Quaternion.identity);
        }
    }
}
