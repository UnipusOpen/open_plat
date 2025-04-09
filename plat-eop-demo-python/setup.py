from setuptools import setup, find_packages

setup(
    name='plat-eop-demo-python',
    version='1.0.0',
    description='A demo project for synchronous audio assessment',
    author='Your Name',
    author_email='your_email@example.com',
    packages=find_packages(),
    install_requires=[
        'requests',
        'pycryptodome'
    ],
    entry_points={
        'console_scripts': [
            'sync_audio_correct=SyncAudioCorrectDemo:main'
        ]
    }
)