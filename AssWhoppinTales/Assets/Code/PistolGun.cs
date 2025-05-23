using UnityEngine;

public class PistolGun : MonoBehaviour
{
    public Transform bulletSpawnPoint;
    public float fireRate = 0.5f;
    private float nextFireTime = 0f;

    void Awake()
    {
        this.enabled = false; // Disable script at start
    }

    void Update()
    {
        if (!this.enabled) return;

        UpdateGunDirection();

        if (Input.GetButtonDown("Fire1"))
        {
            TryShoot();
        }
    }

    void TryShoot()
    {
        if (Time.time >= nextFireTime)
        {
            Shoot();
            nextFireTime = Time.time + fireRate;
        }
    }

    void UpdateGunDirection()
    {
        Vector2 direction = GetMouseDirection().normalized;
        transform.up = direction;
    }

    Vector2 GetMouseDirection()
    {
        Vector2 gunPos = transform.position;
        Vector2 mousePos = Camera.main.ScreenToWorldPoint(Input.mousePosition);
        return mousePos - gunPos;
    }

    public void Shoot()
    {
        GameObject bullet = BulletPool.Instance.GetBullet();
        bullet.transform.position = bulletSpawnPoint.position;
        bullet.transform.rotation = Quaternion.identity;
        bullet.GetComponent<Rigidbody2D>().linearVelocity = GetMouseDirection().normalized * 10f;
    }
}
